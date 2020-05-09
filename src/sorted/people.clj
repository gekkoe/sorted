(ns sorted.people
  (:require [clojure.spec.alpha :as s]
            [failjure.core :as f]
            [sorted.fileio :as file]
            [sorted.person :as p]))

(def people (atom []))
(def post-limit 10000) ; Used to limit online posts to people

(defn load-from-files!
  "Loads files and attempts to parse them as :sorted.person/person values and save
  the collection of them in sorted.people/people. Ignores any lines that it
  cannot parse."
  [files]
  (swap! people (fn [_] (vec (->> (map file/text-read files)
                                  (map (partial map p/str->person))
                                  flatten
                                  (remove f/failed?))))))

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

(s/fdef load-from-files!
  :args (s/cat :files (s/coll-of string? :into []))
  :ret (s/or :success (s/coll-of ::p/person :into [])
             :failure f/failed?))
