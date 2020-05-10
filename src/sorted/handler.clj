(ns sorted.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [join]]
            [compojure.core :refer [defroutes ANY GET]]
            [compojure.route :refer [not-found]]
            [failjure.core :as f]
            [hiccup.core :refer [html]]
            [liberator.core :refer [resource defresource]]
            [prone.middleware :as prone]
            [sorted.helpers :as h]
            [sorted.person :as p]
            [sorted.people :as ppl])
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

(def person-format
  (strs->html
   ["Should be in the form: "
    "Last | First | Gender | FavColor | BirthDate"
    "' | ' can be replaced by ', ' or ' '"
    "Birth date is in M/d/yyyy format."
    "Note: The fields may not contain any of the aformentioned delimeters."]))

(def site-map
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
  "Expects a liberator context with a :sorted.handler/person key containing
  a :sorted.person/person. Returns true if the person is already in the
  people collection."
  [ctx]
  (when (some #{(::person ctx)} @ppl/people)
    [true {:message "Record for this person already exists."}]))

(defn get-content-type
  "Getter for content type in a liberator context."
  [ctx]
  (-> ctx :request :headers (get "content-type")))

(defn get-request-method
  "Getter for request method in a liberator context."
  [ctx]
  (-> ctx :request :request-method))

(defn posting?
  "Returns true if the liberator context ctx is a post request."
  [ctx]
  (= :post (get-request-method ctx)))

(defn check-content-type
  "Returns true if the liberator context ctx is a post request with a content type
  contained in the string collection content-types. Otherwise returns
  a :sorted.handler/false-msg."
  [ctx content-types]
  (if (posting? ctx)
    (if (some #{(get-content-type ctx)} content-types)
      true
      [false {:message (format "Unsupported Content-Type(s)")}])
    true))

(defn check-people-count
  "If the liberator context ctx is a post request but the number of people in the
  people collection is >= the post limit, returns a :sorted.handler/false-msg.
  Otherwise returns true."
  [ctx]
  (if (and (posting? ctx) (>= (count @ppl/people) ppl/post-limit))
    [false {:message "Post not allowed. Records are full."}]
    true))

(defn parse-person-str
  "If the liberator context ctx is not a post request, returns logical false. If
  it is a post request but the body cannot be parsed into a person, returns
  a :sorted.handler/msg-map. Otherwise, returns a :sorted.handler/false-map with
  a map containing a :sorted.handler/person key containing
  the :sorted.person/person parsed from the ctx body. This can then be used
  further down the liberator decision tree."
  [ctx]
  (when (posting? ctx)
    (if-let [body (slurp (-> ctx :request :body))]
      (let [invalid {:message (html [:p "Person string malformed."
                                     person-format])}]
        (if (s/valid? ::p/person-str body)
          (f/if-let-ok?
           [person (p/str->person body)]
           [false {::person person}]
           invalid)
          invalid)))))

;; NOTE: The check of already-exists? here is redundant and may be
;; unnecessary, but I couldn't find any atomicity guarantees in the liberator
;; documentation so I've included it to prevent a possible race condition that
;; would allow duplicates to enter the people collection. From the user's
;; perspective it will appear that they succeeded in adding the person
;; regardless since the resulting people collection will be the same.
(defn post-person!
  "Adds a :sorted.person/person contained in a :sorted.handler/person key of the
  liberator context ctx to the people list if it doesn't already exist. Always
  returns a :sorted.handler/msg-map"
  [ctx]
  (let [person (::person ctx)]
    (swap! ppl/people (fn [old-ps] (if (already-exists? person)
                                     old-ps
                                     (conj old-ps person))))
    {:message {:added (p/person->un-person person)}}))

(defn sorted-people
  "Given a keyword, which should be one of the values in sorted.people/sort-kws,
  returns a sorted :sorted.handler/people-map or, if something goes wrong,
  returns a sorted.handler/error-map"
  [kw]
  (f/if-let-ok? [people (h/ok-map p/person->un-person (ppl/sorted-by kw))]
                {::un-people {:records people}}
                {::error (f/message people)}))

;;;============================================================================
;;;                          R E S O U R C E S
;;;============================================================================
;;; Keys are ordered in accordance with the liberator decision tree for clarity.

(defresource records-resource
  :allowed-methods [:post :get]
  :malformed? parse-person-str
  :allowed? check-people-count
  :known-content-type? #(check-content-type % ["text/plain"])
  :available-media-types ["text/html" "application/json"]
  :post-to-existing? posting?
  :conflict? already-exists?
  :post! post-person!
  :handle-ok (html [:p "Post text/plain to this resource." person-format]))

(defn sorted-resource
  [kw]
  (resource
   :available-media-types ["application/json"]
   :exists? (fn [_] (sorted-people kw))
   :etag (fn [_] (str (count @ppl/people)))
   :handle-ok ::un-people
   :handle-not-found ::error))

;;;============================================================================
;;;                                A P P
;;;============================================================================

(defroutes app
  (GET "/" [] site-map)
  (ANY "/records" [] records-resource)
  (GET "/records/gender" [] (sorted-resource ::p/gender))
  (GET "/records/birthdate" [] (sorted-resource ::p/dob))
  (GET "/records/name" [] (sorted-resource ::p/last-name))
  (not-found (html [:p "Page not found." site-map]) ))

(def prone-enabled? (= "true" (System/getProperty "prone.enabled")))
(def handler
  (cond-> app
    prone-enabled? prone/wrap-exceptions))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(def content-type-gen (s/gen #{"text/html" "text/plain" "application/json"}))
(def content-type-map-gen
  (gen/fmap (fn [c-type] {"content-type" c-type})
            (gen/one-of [content-type-gen (s/gen string?)])))
(s/def ::content-type (s/with-gen string? (constantly content-type-gen)))
(s/def ::content-types (s/coll-of ::content-type :into []))
(s/def ::message (s/or :str string? :map map?))
(s/def ::msg-map (s/keys :req-un [::message]))
(s/def ::false-map (s/tuple false? map?))
(s/def ::true-map  (s/tuple true?  map?))
(s/def ::false-msg (s/tuple false? ::msg-map))
(s/def ::true-msg  (s/tuple true?  ::msg-map))
(s/def ::headers (s/with-gen map? (constantly content-type-map-gen)))
(s/def ::request-method #{:post :get})
(s/def ::body (s/with-gen any?
                #(gen/fmap (fn [p] (ByteArrayInputStream. (.getBytes p)))
                           (s/gen ::p/person-str))))
(s/def ::request (s/keys :req-un [::headers ::request-method ::body]))
(s/def ::ctx (s/keys :req-un [::request]))
;; Make sure that the person in ::person sometimes matches one in ppl/people
;; if there are any.
(s/def ::person (s/with-gen ::p/person
                #(gen/one-of [(s/gen ::p/person)
                              (if (pos? (count @ppl/people))
                                (gen/elements @ppl/people)
                                (s/gen ::p/person))])))
(s/def ::person-map (s/keys :req [::person]))
(s/def ::person-ctx (s/keys :req [::person] :req-un [::request]))
(s/def ::person-kw #{::person})
(s/def ::records (s/* ::p/un-person))
(s/def ::un-people (s/keys :req-un [::records]))
(s/def ::un-people-map (s/keys :req [::un-people]))
(s/def ::error string?)
(s/def ::error-map (s/keys :req [::error]))

(s/fdef strs->html
  :args (s/cat :ss (s/and (s/coll-of string? :into [])))
  :ret string?)

(s/fdef link
  :args (s/cat :s string?)
  :ret string?)

(s/fdef already-exists?
  :args (s/cat :ctx ::person-ctx)
  :ret (s/or :false (s/nilable false?)
             :true  ::true-msg))

(s/fdef get-content-type
  :args (s/cat :ctx ::ctx)
  :ret (s/nilable string?))

(s/fdef get-request-method
  :args (s/cat :ctx ::ctx)
  :ret (s/nilable keyword?))

(s/fdef posting?
  :args (s/cat :ctx ::ctx)
  :ret boolean?)

(s/fdef check-content-type
  :args (s/cat :ctx ::ctx :content-types ::content-types)
  :ret (s/or :false ::false-msg
             :true  (s/and boolean? true?)))

(s/fdef check-people-count
  :args (s/cat :ctx ::ctx)
  :ret (s/or :false ::false-msg
             :true  (s/and boolean? true?)))

(s/fdef parse-person-str
  :args (s/cat :ctx ::ctx)
  :ret (s/or :success ::false-map
             :failure (s/nilable ::msg-map))
  :fn #(let [result (-> % :ret first)
             ret (-> % :ret second)]
         (if (= result :success)
           (s/valid? ::p/person (-> ret second ::person))
           true)))

(s/fdef post-person!
  :args (s/cat :ctx ::person-ctx)
  :ret ::msg-map
  :fn #(let [person (-> % :args :ctx ::person)
             added (-> % :ret :message second :added)]
         (and (= (p/person->un-person person) added)
              (some #{person} @ppl/people))))

(s/fdef sorted-people
  :args (s/cat :kw ::ppl/sort-kw)
  :ret (s/or :success ::un-people-map
             :failure ::error-map))

(comment
  (handler/handler
   (->
    (rm/request :post "/records" "Baronson,Jill,Female,Violet,10/10/1949" )
    (rm/content-type "text/plain")))
  )
