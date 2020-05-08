(ns sorted.handler
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :refer [join]]
            [compojure.core :refer [defroutes ANY GET POST]]
            [compojure.route :refer [not-found]]
            [sorted.person :as p]
            [sorted.people :refer [sorted-by]]
            [sorted.people :as ppl]
            [clojure.spec.alpha :as s]
            [failjure.core :as f])
  (:import java.io.StringWriter))

(defn pprint->string [x]
  (let [w (StringWriter.)]
    (pprint x w)
    (.toString w)))

(def site-map-msg
  (->> ["This site supports the following operations:"
        "POST <a href=\"/records\">/records</a> - Add a person to the site."
        (str "GET <a href=\"/records/gender\">/records/gender</a>"
             " - Sort by gender, female first.")
        (str "GET <a href=\"/records/birthdate\">/records/birthdate</a>"
             " - Sort by birth date ascending, then by last name ascending.")
        (str "GET <a href=\"/records/name\">/records/name</a>"
             " - Sort by last name descending.")]
       (join "<br>")))

(defn sorted-resource
  [kw]
  (resource
   :available-media-types ["application/json"]
   :exists? (fn [ctx]
              (try
                {::data {:people (map p/person->un-map (sorted-by kw))}}
                (catch Exception e
                  [false {::error (.getMessage ^Throwable e)}])))
   :handle-ok ::data
   :handle-not-found ::error
   :etag (fn [_] (str (count @ppl/people)))))

(defn parse-person-str [ctx key]
  (when (= :post (-> ctx :request :request-method))
    (if-let [body (slurp (-> ctx :request :body))]
      (if (s/valid? ::p/person-str body)
        (f/if-let-ok?
         [person (p/str->person body)]
         [false {key person}]
         {:message (str "Person string malformed. Should be in the form "
                        "Last|First|Gender|FavColor|BirthDate "
                        "where | can be replaced by space or comma and "
                        "birth date is in M/d/yyyy format.")})))))

(defroutes app
  (GET "/" request site-map-msg)
  (ANY "/records" []
       (resource
        :allowed-methods [:post :get]
        :available-media-types ["text/html" "application/json"]
        :handle-ok "<html>Post text/plain to this resource.<br>\n"
        :malformed? #(parse-person-str % ::data)
        :post! (fn [ctx] (swap! ppl/people conj (::data ctx)))))
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

(comment
  (require '[ring.mock.request :as rm])
  (require '[sorted.handler :as handler])
  (handler/handler (rm/body (rm/request :post "/records") "Aaronson,Jill,Female,Violet,10/10/1979"))
  )
