(ns sorted.person-test
  (:require [clojure.test :refer :all]
            [sorted.person :as p]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as g]
            [java-time :as jt]
            [clojure.spec.gen.alpha :as gen]))

(def num-samples 1000) ; Increase this to run slower but more exhaustive tests.
(def formatter "MM/dd/yyyy")
(def min-date (jt/local-date formatter "01/01/0001"))
(def max-date (jt/local-date formatter "12/31/9999"))

(deftest person-test
  (let [valid-person {::p/last-name "Doe"
                      ::p/first-name "John"
                      ::p/gender "Male"
                      ::p/fav-color "Blue"
                      ::p/dob (jt/local-date p/formatter "01/22/1972")}
        invalid-person {:x 3 :y 9}]
    (testing "Checking if a person conforms to spec"
      (is (s/valid? ::p/person valid-person)))
    (testing "Checking if an invalid person conforms to spec"
      (is (not (s/valid? ::p/person invalid-person))))))

(deftest delim-str-test
  (let [expected? #{"|" "," " "}]
    (testing "Generated delims are pipes, commas, or spaces"
      (is (every? expected? (gen/sample (s/gen ::p/delim-str) num-samples))))))

(deftest delim-regex-test
  (let [expected? (into #{} (map str #{#"\|" #"," #" "}))]
    (testing "Generated delim regex patterns are pipes, commas, or spaces"
      (is (every? expected? (map str (gen/sample (s/gen ::p/delim-regex) num-samples)))))))

(deftest date-test
  (testing "Generated dates are within range of 01/01/0001 and 12/31/9999"
    (let [expected? #(and (jt/after?  % (jt/minus min-date (jt/days 1)))
                          (jt/before? % (jt/plus max-date (jt/days 1))))]
      (is (every? expected? (gen/sample (s/gen ::p/date) num-samples))))))

(deftest date-str-test
  (testing "Generated date strings are within range of 01/01/0001 and 12/31/9999"
    (let [expected? #(and (jt/after?  (jt/local-date formatter %)
                                      (jt/minus min-date (jt/days 1)))
                          (jt/before? (jt/local-date formatter %)
                                      (jt/plus max-date (jt/days 1))))]
      (is (every? expected? (gen/sample (s/gen ::p/date-str) num-samples))))))

(deftest no-delim-str-test
  (testing "Generated strings do not contain pipes, commas, or spaces"
    (is (not-any? #(re-find #"\||,| " %)
                  (gen/sample (s/gen ::p/no-delim-str) num-samples)))))
