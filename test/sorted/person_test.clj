(ns sorted.person-test
  (:require [clojure.test :refer :all]
            [sorted.person :as p]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as g]
            [clj-time.format :as ctf]))

(deftest person-exercise-test
  (let [us-date (ctf/formatter "MM-dd-yyyy")
        parse-date #(ctf/parse us-date %)
        valid-person {::p/last-name "Doe"
                      ::p/first-name "John"
                      ::p/gender "Male"
                      ::p/fav-color "Blue"
                      ::p/dob (parse-date "01-22-1972")}
        invalid-person {:x 3 :y 9}]
    (testing "Checking if a person conforms to spec"
      (is (s/valid? ::p/person valid-person)))
    (testing "Checking if an invalid person conforms to spec"
      (is (not (s/valid? ::p/person invalid-person))))))
