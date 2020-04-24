(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [sorted.config :refer [env]]
   [clj-time.format :as ctf]
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]
   [clojure.spec.test.alpha :as st]
   [clojure.java.classpath :refer [classpath]]
   [expound.alpha :as expound]
   [failjure.core :as f]
   #_[mount.core :as mount]
   [sorted.core :refer :all]
   [sorted.person :as p]
   [sorted.fileio :as file]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

;; Some reusable vars for use during development.
(def john "Doe John Male Blue 1/1/1943")
(def jane "Doe, Jane, Female, Green,     12/1/2001")
(def june "Doe| June| Female| Red   | 2/1/1983")
(def jim "Doe| Jim|Male||         11/21/2014")

;; Some malformed person string
(def bad1 "Doe| Jack|Male| Orange |111/21/2014")
(def bad2 "Doe| Jenny|Female| Pink |")
(def bad3 "_,|| |,|")
(def bad4 "")

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
