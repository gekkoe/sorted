(ns sorted.person-test
  (:require [clojure.test :refer :all]
            [sorted.person :as p]
            [sorted.helpers :as h]
            [clojure.spec.alpha :as s]
            [java-time :as jt]
            [java-time.local]
            [clojure.spec.gen.alpha :as gen]
            [failjure.core :as f]))

(def num-samples 1000) ; Amount of sample data to produce.
(def gen-samples (h/gen-samples num-samples))
(def num-tests 1000)    ; Number of times to check function specs.
(def checks? (h/checks? num-tests))

(def min-date (jt/local-date p/formatter "1/1/0001"))
(def max-date (jt/local-date p/formatter "12/31/9999"))

(def num-person-fields 5)

(def valid-person {::p/last-name "Doe"
                   ::p/first-name "Jane"
                   ::p/gender "Female"
                   ::p/fav-color "Red"
                   ::p/dob (jt/local-date p/formatter "1/1/1949")})
(def invalid-person {:a 3
                     ::p/last-name "something"
                     :b ["xyz"]
                     ::p/fav-color 9})
(def valid-person-str "Doe, Jane , Female, Red, 1/1/1949  ")
(def valid-person-vec ["Doe" "Jane" "Female" "Red" "1/1/1949"])
(def valid-person-vals-vec ["Doe"
                            "Jane"
                            "Female"
                            "Red"
                            (jt/local-date p/formatter "1/1/1949")])

(s/def ::no-strs (s/and any? (complement string?)))

(deftest delim-str-test
  (let [expected? p/delim-str-set]
    (testing "Generated strings contain expected delims"
      (is (every? expected? (gen-samples ::p/delim-str))))))

(deftest delim-regex-test
  (let [expected? p/delim-regex-str-set]
    (testing "Generated delim regex patterns are expected delims"
      (is (every? expected? (map str (gen-samples ::p/delim-regex)))))))

(deftest date-test
  (testing "Generated dates are within range of 1/1/0001 and 12/31/9999"
    (let [expected? #(and (jt/after?  % (jt/minus min-date (jt/days 1)))
                          (jt/before? % (jt/plus max-date (jt/days 1))))]
      (is (every? expected? (gen-samples ::p/date))))))

(deftest date-str-test
  (testing "Generated date strings are within range of 1/1/0001 and 12/31/9999"
    (let [expected? #(and (jt/after?  (jt/local-date p/formatter %)
                                      (jt/minus min-date (jt/days 1)))
                          (jt/before? (jt/local-date p/formatter %)
                                      (jt/plus max-date (jt/days 1))))]
      (is (every? expected? (gen-samples ::p/date-str))))))

(deftest no-delim-str-test
  (testing "Generated strings do not contain delims"
    (is (not-any? #(re-find p/delim-regex %) (gen-samples ::p/no-delim-str)))))

(deftest no-delim-specs-test
  (testing "Expected specs conform to :sorted.person/no-delim-str."
    (let [verified? (h/verified? ::p/no-delim-str num-samples)]
      (is (verified? ::p/last-name))
      (is (verified? ::p/first-name))
      (is (verified? ::p/gender))
      (is (verified? ::p/fav-color)))))

(deftest dob-test
  (testing "Conforms to :sorted.person/date"
    (is (h/verified? ::p/dob ::p/date num-samples))))

(deftest person-test
  (testing "Checking if a valid person conforms to spec"
    (is (s/valid? ::p/person valid-person)))
  (testing "Checking if an invalid person conforms to spec"
    (is (not (s/valid? ::p/person invalid-person)))))

(deftest person-strs-test
  (let [samps (gen-samples ::p/person-strs)]
    (testing "Generated values are vectors"
      (is (every? vector? samps))
      (testing "of strings"
        (is (every? #(every? string? %) samps)))
      (testing "containing expected number of values"
        (is (every? #(= (count %) num-person-fields) samps))))))

(s/def ::person-vals-shape
  (s/tuple string? string? string? string? #(instance? java.time.LocalDate %)))
(deftest person-vals-test
  (let [samps (gen-samples ::p/person-vals)]
    (testing "Generated values are vectors"
      (is (every? vector? samps))
      (testing "of string string string string LocalDate"
        (is (every? #(s/valid? ::person-vals-shape %) samps))))))

(deftest person-str-test
  (let [samps (gen-samples ::p/person-str)]
    (testing "Generated values are strings"
      (is (every? string? samps))
      (testing "that do not contain line breaks or carriage returns"
        (is (not-any? #(re-find p/line-breaks %) samps)))
      (testing "that contains one of the expected delims"
        (is (every? #(re-find p/delim-regex %) samps))))))

(deftest get-delim-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/get-delim)))
  (testing "Returns a delim when passed a string containing one or more"
    (is (every? p/delim-str-set (map p/get-delim (gen-samples ::p/person-str)))))
  (testing "Returns nil when"
    (testing "passed a string with no delims"
      (is (every? nil? (map  p/get-delim (gen-samples ::p/no-delim-str)))))
    (testing "passed a non-string arg"
      (is (every? nil? (map p/get-delim (gen-samples ::no-strs)))))))

(deftest split-trim-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/split-trim)))
  (testing "Returns expected vector when passed good args"
    (is (= valid-person-vec (p/split-trim valid-person-str #","))))
  (testing "Returns a Failure object when passed bad args"
    (let [ret (p/split-trim "NotA,ValidPerson" #",")]
      (is (f/failed? ret))
      (testing "containing the expected error message"
        (is (= (str "Error in split-trim: Could not split person "
                    "`NotA,ValidPerson` with delim `,`")
               (f/message ret)))))))

(deftest split-on-delims-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/split-on-delims)))
  (testing "Returns expected vector when passed good args"
    (testing "when passed no delim"
      (is (= valid-person-vec (p/split-on-delims valid-person-str))))
    (testing "when passed correct delim"
      (is (= valid-person-vec (p/split-on-delims valid-person-str ",")))))
  (testing "Returns a Failure object"
    (testing "when wrong delim is passed"
      (is (f/failed? (p/split-on-delims valid-person-str "|"))))
    (testing "when passed a bad s arg"
      (is (f/failed? (p/split-on-delims "Bad,Arg")))
      (testing "even if passed the right delim"
        (is (f/failed? (p/split-on-delims "Bad,Arg" ",")))))
    (testing "containing the expected error message"
      (is (= (str "Error in split-on-delims: Could not parse `Bad,Arg`\nError "
                  "in split-trim: Could not split person `Bad,Arg` with delim "
                  "`,`")
             (f/message (p/split-on-delims "Bad,Arg")))))))

(deftest strs->vals-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/strs->vals)))
  (testing "Returns a valid :sorted.person/person-vals when passed a good arg"
    (is (s/valid? ::p/person-vals (p/strs->vals valid-person-vec))))
  (testing "Returns a Failure object when passed a bad arg"
    (let [ret (p/strs->vals ["this" "is" "invalid"])]
      (is (f/failed? ret))
      (testing "containing the expected error message"
        (is (= (str "Error in str->vals: Could not parse "
                    "`[\"this\" \"is\" \"invalid\"]`\nConversion failed")
               (f/message ret)))))))

(deftest vals->person-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/vals->person)))
  (testing "Returns a valid :sorted.person/person when passed a good arg"
    (is (s/valid? ::p/person (p/vals->person valid-person-vals-vec))))
  (testing "Returns a Failure object when passed a bad arg"
    (let [ret (p/vals->person ["this" "is" "invalid"])]
      (is (f/failed? ret))
      (testing "containing the expected error message"
        (is (= (str "Error in vals->person: `[\"this\" \"is\" \"invalid\"]` is "
                    "not a valid :sorted.person/person-vals.")
               (f/message ret)))))))

(deftest str->person-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/str->person)))
  (testing "Returns a valid person when given good args"
    (is (s/valid? ::p/person (p/str->person valid-person-str))))
  (testing "Returns a Failure object when given bad args"
    (let [ret (p/str->person "bad argument")]
      (is (f/failed? ret))
      (testing "containing the expected error message"
        (is (= (str "Error in str->person: Could not parse `bad argument`\n"
                    "Error in split-on-delims: Could not parse `bad argument`\n"
                    "Error in split-trim: Could not split person `bad argument` "
                    "with delim ` `") (f/message ret)))))))

(deftest person->str-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/person->str)))
  (testing "Returns a valid person-str when given good args"
    (is (s/valid? ::p/person-str (p/person->str valid-person " "))))
  (testing "Returns a Failure object when given bad args"
    (let [ret (p/person->str invalid-person " ")]
      (is (f/failed? ret))
      (testing "containing the expected error message"
        (is (= (str "Error in person->str: `{:a 3, :sorted.person/last-name "
                    "\"something\", :b [\"xyz\"], :sorted.person/fav-color 9}` "
                    "is not a valid :sorted.person/person.")
               (f/message ret)))))))

(deftest person->un-person-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.person/person->un-person)))
  (testing "Returns a valid un-person when given good args"
    (is (s/valid? ::p/un-person (p/person->un-person valid-person))))
  (testing "Returns a Failure object when given bad args"
    (let [ret (p/person->un-person invalid-person)]
      (is (f/failed? ret))
      (testing "containing the expected error message"
        (is (= (str "Error in person->un-person: Unable to create an "
                    "unqualified map representing person `{:a 3, "
                    ":sorted.person/last-name \"something\", :b [\"xyz\"], "
                    ":sorted.person/fav-color 9}`")
               (f/message ret)))))))
