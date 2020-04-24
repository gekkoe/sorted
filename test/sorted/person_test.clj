(ns sorted.person-test
  (:require [clojure.test :refer :all]
            [sorted.person :as p]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as g]
            [java-time :as jt]))

(deftest person-exercise-test
  (let [valid-person {::p/last-name "Doe"
                      ::p/first-name "John"
                      ::p/gender "Male"
                      ::p/fav-color "Blue"
                      ::p/dob (p/str->java-date "01/22/1972")}
        invalid-person {:x 3 :y 9}]
    (testing "Checking if a person conforms to spec"
      (is (s/valid? ::p/person valid-person)))
    (testing "Checking if an invalid person conforms to spec"
      (is (not (s/valid? ::p/person invalid-person))))))
