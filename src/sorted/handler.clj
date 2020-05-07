(ns sorted.handler
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [cheshire.core :refer :all]
            [clojure.pprint :refer [pprint]]
            [compojure.core :refer [defroutes GET POST]]
            [sorted.person :as p]
            [sorted.people :refer [sorted-by]])
  (:import java.io.StringWriter))

(defn pprint->string [x]
  (let [w (StringWriter.)]
    (pprint x w)
    (.toString w)))

(defn sorted-resource
  [kw]
  (resource
   :available-media-types ["application/json"]
   :exists? (fn [ctx]
              (try
                {::data (generate-string
                         {:people (map p/person->un-map
                                       (sorted-by kw))})}
                (catch Exception e
                  [false {::error (.getMessage ^Throwable e)}])))
   :handle-ok ::data
   :handle-not-found ::error))

(defroutes app
  (GET "/records/gender" request
       (sorted-resource ::p/gender))
  (GET "/records/birthdate" request
       (sorted-resource ::p/dob))
  (GET "/records/name" request
       (sorted-resource ::p/last-name))
  (GET "/test" request
       (pprint->string request)))

(def handler
  (-> app
      wrap-params))
