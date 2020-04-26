(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [sorted.config :refer [env]]
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.spec.test.alpha :as st]
   [clojure.java.classpath :refer [classpath]]
   [expound.alpha :as expound]
   [failjure.core :as f]
   [java-time :as jt]
   #_[mount.core :as mount]
   [sorted.core :refer :all]
   [sorted.person :as p]
   [sorted.fileio :as file]))

;; KLUDGE: For some reason this can't be found on the classpath if I just
;;   include it in the ns statment above. It's convenient to have it aliased
;;   for st/check calls though.
(alias 'stc 'clojure.spec.test.check)

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn ex-check
  ([spec]
   (expound/explain-results (st/check spec)))
  ([spec num-tests]
   (expound/explain-results (st/check spec {:clojure.spec.test.check/opts {:num-tests num-tests}}))))

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
