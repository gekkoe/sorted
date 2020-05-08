(ns sorted.people
  (:require [clojure.spec.alpha :as s]
            [sorted.person :as p]))

(def people (atom []))

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

(s/def ::sort-kw (s/with-gen (s/nilable keyword?)
                             (constantly (s/gen #{::p/gender
                                                   ::p/dob
                                                   ::p/last-name
                                                   nil}))))

(s/fdef sorted-by
  :args (s/cat :sort-kw ::sort-kw)
  :ret (s/coll-of ::p/person :into []))
