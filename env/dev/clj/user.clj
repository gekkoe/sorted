(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [sorted.config :refer [env]]
    [clojure.pprint]
    [clojure.spec.alpha :as s]
    [expound.alpha :as expound]
    #_[mount.core :as mount]
    #_[sorted.core :refer [start-app]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(def valid? s/valid?)

#_(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'sorted.core/repl-server))

#_(defn stop
  "Stops application."
  []
  (mount/stop-except #'sorted.core/repl-server))

#_(defn restart
  "Restarts application."
  []
  (stop)
  (start))
