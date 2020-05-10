(ns sorted.people
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [failjure.core :as f]
            [sorted.fileio :as file]
            [sorted.helpers :as h]
            [sorted.person :as p]))

(def people (atom []))
(def post-limit 10000) ; Used to limit online posts to people

(defn load-from-files!
  "Expects a collection of strings containing file names. Loads files and attempts
  to parse them as :sorted.person/person values and save the collection of them
  in sorted.people/people. Ignores any lines that it cannot parse."
  [files]
  (reset! people (->> (map file/text-read files)
                      flatten
                      (map p/str->person)
                      (remove f/failed?)
                      vec)))

(defn sorted-by
  [sort-kw]
  (when-let [comparator
             (case sort-kw
               ::p/dob       (fn [x y] (compare [(sort-kw x) (::p/last-name x)]
                                                [(sort-kw y) (::p/last-name y)]))
               ::p/gender    (fn [x y] (compare (sort-kw x) (sort-kw y)))
               ::p/last-name (fn [x y] (compare (sort-kw y) (sort-kw x)))
               (constantly 0))] ; if no valid kw, just don't sort
    (vec (sort comparator @people))))

(defn people->strs [ppl] (map #(p/person->str % " ") ppl))

(def sort-kws #{::p/gender ::p/dob ::p/last-name})
(s/def ::sort-kw (s/with-gen (s/nilable keyword?)
                   #(gen/one-of [(s/gen sort-kws)
                                 (gen/return nil)])))

(s/fdef sorted-by
  :args (s/cat :sort-kw ::sort-kw)
  :ret (s/coll-of ::p/person :into []))

(s/fdef load-from-files!
  :args (s/cat :files (s/coll-of string? :into []))
  :ret (s/or :success (s/coll-of ::p/person :into [])
             :failure f/failed?)
  :fn #(if (= (-> % :ret first) :success)
         (= (-> % :ret second) @people)
         true))
