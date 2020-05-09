(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.spec.test.alpha :as st]
   [clojure.java.classpath :refer [classpath]]
   [expound.alpha :as expound]
   [failjure.core :as f]
   [java-time :as jt]
   [ring.mock.request :as rm]
   [sorted.core :as c]
   [sorted.fileio :as file]
   [sorted.handler :as handler]
   [sorted.helpers :as h]
   [sorted.people :as ppl]
   [sorted.person :as p]
   [sorted.server :as svr]
   [java-time-literals.core]))

;; KLUDGE: For some reason this can't be found on the classpath if I just
;;   include it in the ns statment above. It's convenient to have it aliased
;;   for st/check calls though.
(alias 'stc 'clojure.spec.test.check)

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(def space-delim-file "env/dev/resources/space-delim")
(def comma-delim-file "env/dev/resources/comma-delim")
(def pipe-delim-file "env/dev/resources/pipe-delim")

(defn main
  [& args]
  (with-redefs [sorted.core/system-exit
                (fn [status]
                  (printf "System Exit Overridden by User.clj. Status: %s\n"
                          status))]
    (apply c/-main args)))

(defn check
  ([spec] (check spec 1000))
  ([spec num-tests]
   (st/check spec {:clojure.spec.test.check/opts {:num-tests num-tests}})))

(defn ex-check
  ([spec] (ex-check spec 1000))
  ([spec num-tests]
   (expound/explain-results (check spec num-tests))))

(defn check-ns
  ([] (check-ns 'sorted.person 1000))
  ([my-ns] (check-ns my-ns 1000))
  ([my-ns num-tests]
   (map first (remove empty? (map #(check % num-tests)
                                  (st/enumerate-namespace my-ns))))))

(defn ex-check-ns
  ([] (ex-check-ns 'sorted.person 1000))
  ([my-ns] (ex-check-ns my-ns 1000))
  ([my-ns num-tests]
   (expound/explain-results (check-ns my-ns num-tests))))
