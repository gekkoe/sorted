(ns sorted.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [join]]
            [compojure.core :refer [defroutes ANY GET]]
            [compojure.route :refer [not-found]]
            [failjure.core :as f]
            [hiccup.core :refer [html]]
            [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [sorted.person :as p]
            [sorted.people :as ppl]
            [clojure.spec.gen.alpha :as gen])
  (:import (java.io ByteArrayInputStream)))

;;;============================================================================
;;;                          C O N T E N T
;;;============================================================================

(defn strs->html
  "Converts a vector of strings to simple HTML."
  [ss]
  (html [:div (map #(vector :p %) ss)]))

(defn link
  "Converts a URI to an HTML link."
  [s]
  (html [:a {:href s} s]))

(def person-format-msg
  (strs->html
   ["Should be in the form: "
    "Last | First | Gender | FavColor | BirthDate"
    "' | ' can be replaced by ', ' or ' '"
    "Birth date is in M/d/yyyy format."
    "Note: The fields may not contain any of the aformentioned delimeters."]))

(def site-map-msg
  (strs->html
   ["This site supports the following operations:"
    (str "POST " (link "/records") " - Add a person to the records list.")
    (str "GET "  (link "/records/gender") " - Sort by gender, female first.")
    (str "GET "  (link "/records/birthdate")
         " - Sort by birth date ascending, then by last name ascending.")
    (str "GET "  (link "/records/name") " - Sort by last name descending.")]))

;;;============================================================================
;;;                          H E L P E R S
;;;============================================================================

(defn already-exists?
  [ctx]
  (when (some #{(::data ctx)} @ppl/people)
    [true {:message "Record for this person already exists."}]))

(defn posting?
  [ctx]
  (= :post (-> ctx :request :request-method)))

(defn check-content-type [ctx content-types]
  (if (posting? ctx)
    (let [content-type (get-in ctx [:request :headers "content-type"])]
      (if-not (some #{content-type} content-types)
        [false {:message (format
                          "Unsupported Content-Type%s"
                          (if content-type
                            (str ": " content-type)
                            ""))}]
        true))
    true))

(defn check-people-count
  [ctx]
  (let [method (get-in ctx [:request :request-method])]
    (case method
      :get true
      :post (if (< (count @ppl/people) ppl/post-limit)
              true
              [false {:message "Post not allowed. Records are full."}])
      false)))

(defn parse-person-str [ctx kw]
  (when (posting? ctx)
    (if-let [body (slurp (-> ctx :request :body))]
      (let [invalid {:message (html [:p "Person string malformed."
                                     person-format-msg])}]
        (if (s/valid? ::p/person-str body)
          (f/if-let-ok?
           [person (p/str->person body)]
           (do (println body)
             [false {kw person}])
           invalid)
          invalid)))))

;;;============================================================================
;;;                          R E S O U R C E S
;;;============================================================================

(defresource records-resource
  :available-media-types ["application/json"]
  :allowed? #(check-people-count %)
  :allowed-methods [:post :get]
  :known-content-type? #(check-content-type % ["text/plain"])
  :handle-ok (str "<html>Post text/plain to this resource.<br><br>"
                  person-format-msg)
  :malformed? #(parse-person-str % ::data)
  :post-to-existing? #(posting? %)
  :conflict? #(already-exists? %)
  :post! (fn [ctx]    ; already-exists? needed to prevent race condition here?
           (let [data (::data ctx)]
             (swap! ppl/people #(if (already-exists? data) % (conj % data)))
             {:message {:added (p/person->un-map data)}})))

(defn sorted-resource
  [kw]
  (resource
   :available-media-types ["application/json"]
   :exists? (fn [ctx]
              (try
                {::data {:people (map p/person->un-map (ppl/sorted-by kw))}}
                (catch Exception e
                  [false {::error (.getMessage ^Throwable e)}])))
   :handle-ok ::data
   :handle-not-found ::error
   :etag (fn [_] (str (count @ppl/people)))))

;;;============================================================================
;;;                                A P P
;;;============================================================================

(defroutes app
  (GET "/" request site-map-msg)
  (ANY "/records" [] records-resource)
  (GET "/records/gender" request
       (sorted-resource ::p/gender))
  (GET "/records/birthdate" request
       (sorted-resource ::p/dob))
  (GET "/records/name" request
       (sorted-resource ::p/last-name))
  (not-found (str "Page not found.<br>" site-map-msg) ))

(def handler
  (-> app
      wrap-params))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(def content-type-gen (s/gen #{"text/html" "text/plain" "application/json"}))
(def content-type-map-gen
  (gen/fmap (fn [kw] {"content-type" kw})
            (gen/one-of [content-type-gen (s/gen string?)])))
(s/def ::content-type (s/with-gen string? (constantly content-type-gen)))
(s/def ::content-types (s/coll-of ::content-type :into []))
(s/def ::message string?)
(s/def ::msg-map (s/keys :req-un [::message]))
(s/def ::headers (s/with-gen map? (constantly content-type-map-gen)))
(s/def ::request-method #{:post :get})
(s/def ::body  (s/with-gen any?
                 #(gen/fmap (fn [p] (ByteArrayInputStream. (.getBytes, p)))
                            (s/gen ::p/person-str))))
(s/def ::request (s/keys :req-un [::headers ::request-method ::body]))
(s/def ::ctx (s/keys :req-un [::request]))
;; Make sure that the person in ::data sometimes matches one in ppl/people
;; if there are any.
(s/def ::data (s/with-gen ::p/person
                #(gen/one-of [(s/gen ::p/person)
                              (if (pos? (count @ppl/people))
                                (gen/elements @ppl/people)
                                (s/gen ::p/person))])))
(s/def ::data-map (s/keys :req [::data]))
(s/def ::data-ctx (s/keys :req [::data] :req-un [::request]))
(s/def ::data-kw #{::data})

(s/fdef strs->html
  :args (s/cat :ss (s/and (s/coll-of string? :into [])))
  :ret string?)

(s/fdef link
  :args (s/cat :s string?)
  :ret string?)

(s/fdef already-exists?
  :args (s/cat :ctx ::data-ctx)
  :ret (s/or :false (s/nilable false?)
             :true  (s/tuple true? ::msg-map)))

(s/fdef posting?
  :args (s/cat :ctx ::ctx)
  :ret boolean?)

(s/fdef check-content-type
  :args (s/cat :ctx ::ctx :content-types ::content-types)
  :ret (s/or :false (s/tuple false? ::msg-map)
             :true  (s/and boolean? true?)))

(s/fdef check-people-count
  :args (s/cat :ctx ::ctx)
  :ret (s/or :false (s/tuple false? ::msg-map)
             :true  (s/and boolean? true?)))

(s/fdef parse-person-str
  :args (s/cat :ctx ::ctx :key ::data-kw)
  :ret (s/or :success (s/tuple false? ::data-map)
             :failure (s/nilable ::msg-map)))

(comment
  (handler/handler
   (->
    (rm/request :post "/records" "Baronson,Jill,Female,Violet,10/10/1949" )
    (rm/content-type "text/plain")))
  )
