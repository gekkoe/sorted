(ns sorted.person-test
  (:require [clojure.test :refer :all]
            [sorted.person :as p]
            [sorted.helpers :as h]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as g]
            [java-time :as jt]
            [clojure.spec.gen.alpha :as gen]
            [failjure.core :as f]))

(def num-samples 1000) ; Increase this to run slower but more exhaustive tests.
(def samples #(h/gen-samples % num-samples))

(def num-tests 100)    ; Number of times to check function specs.

(def formatter "MM/dd/yyyy")
(def min-date (jt/local-date formatter "01/01/0001"))
(def max-date (jt/local-date formatter "12/31/9999"))

(def num-person-fields 5)

(deftest delim-str-test
  (let [expected? p/delim-str-set]
    (testing "Generated strings contain expected delims"
      (is (every? expected? (samples ::p/delim-str))))))

(deftest delim-regex-test
  (let [expected? p/delim-regex-str-set]
    (testing "Generated delim regex patterns are expected delims"
      (is (every? expected? (map str (samples ::p/delim-regex)))))))

(deftest date-test
  (testing "Generated dates are within range of 01/01/0001 and 12/31/9999"
    (let [expected? #(and (jt/after?  % (jt/minus min-date (jt/days 1)))
                          (jt/before? % (jt/plus max-date (jt/days 1))))]
      (is (every? expected? (samples ::p/date))))))

(deftest date-str-test
  (testing "Generated date strings are within range of 01/01/0001 and 12/31/9999"
    (let [expected? #(and (jt/after?  (jt/local-date formatter %)
                                      (jt/minus min-date (jt/days 1)))
                          (jt/before? (jt/local-date formatter %)
                                      (jt/plus max-date (jt/days 1))))]
      (is (every? expected? (samples ::p/date-str))))))

(deftest no-delim-str-test
  (testing "Generated strings do not contain delims"
    (is (not-any? #(re-find p/delim-regex %) (samples ::p/no-delim-str)))))

(deftest no-delim-specs-test
  (testing "Expected specs conform to :sorted.person/no-delim-str."
    (let [verified? #(h/verified? % ::p/no-delim-str num-samples)]
      (is (verified? ::p/last-name))
      (is (verified? ::p/first-name))
      (is (verified? ::p/gender))
      (is (verified? ::p/fav-color)))))

(deftest dob-test
  (testing "Conforms to :sorted.person/date"
    (is (h/verified? ::p/dob ::p/date num-samples))))

(deftest person-test
  (let [valid-person {::p/last-name "Doe"
                      ::p/first-name "John"
                      ::p/gender "Male"
                      ::p/fav-color "Blue"
                      ::p/dob (jt/local-date p/formatter "01/22/1972")}
        invalid-person {:x 3 :y 9}]
    (testing "Checking if a valid person conforms to spec"
      (is (s/valid? ::p/person valid-person)))
    (testing "Checking if an invalid person conforms to spec"
      (is (not (s/valid? ::p/person invalid-person))))))

(deftest person-strs-test
  (let [samps (samples ::p/person-strs)]
    (testing "Generated values are vectors"
      (is (every? vector? samps))
      (testing "of strings"
        (is (every? #(every? string? %) samps)))
      (testing "containing expected number of values"
        (is (every? #(= (count %) num-person-fields) samps))))))

(s/def ::person-vals-shape
  (s/tuple string? string? string? string? #(instance? java.time.LocalDate %)))
(deftest person-vals-test
  (let [samps (samples ::p/person-vals)]
    (testing "Generated values are vectors"
      (is (every? vector? samps))
      (testing "of string string string string LocalDate"
        (is (every? #(s/valid? ::person-vals-shape %) samps))))))

(deftest person-str-test
  (let [samps (samples ::p/person-str)]
    (testing "Generated values are strings"
      (is (every? string? samps))
      (testing "that do not contain line breaks or carriage returns"
        (is (not-any? #(re-find p/line-breaks %) samps)))
      (testing "that contains one of the expected delims"
        (is (every? #(re-find p/delim-regex %) samps))))))

(deftest str->person-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.person/str->person num-tests))))

(deftest person->str-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.person/person->str num-tests))))

(deftest no-delims?-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.person/no-delims? num-tests))))

(deftest split-trim-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.person/split-trim num-tests))))

(deftest split-on-delims-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.person/split-on-delims num-tests))))

(deftest strs->vals-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.person/strs->vals num-tests))))

(deftest vals->person-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.person/vals->person num-tests))))
