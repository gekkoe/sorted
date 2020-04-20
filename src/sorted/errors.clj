(ns sorted.errors
  (:require [clojure.spec.alpha :as s]))

(s/def ::message string?)
(s/def ::error (s/keys :req [::message]))

(defn err-msg
  "Returns an ::error containing the message in an exception.
  If f is provided, the message will indicate that the error occurred in the f
  function."
  [e f]
  (try
    {::message (str (str "Error" (when f (str " in " f)) ": ")
                    (.getMessage e))}
    (catch Exception ex
      (err-msg ex))))
