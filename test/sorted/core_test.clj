(ns sorted.core-test
  (:require [clojure.test :refer :all]
            [sorted.core :as c]
            [sorted.helpers :as h]
            [sorted.person :as p]))

(def num-tests 1000) ; Increase this to run slower but more exhaustive tests.
(def checks? (h/checks? num-tests))

(def jane "Doe Jane Female Purple 1/1/2000")
(def june "Darla June Female Orange 1/1/2000")
(def john "Dean John Male Blue 12/31/2001")
(def sam  "Francis Sam Male Green 2/11/1994")
(def malformed "Some Invalid Record")
(def test-file [jane june john sam malformed])

(defn people->strs [people] (map #(p/person->str % " ") people))

(deftest usage-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.core/usage))))

(deftest error-msg-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.core/error-msg))))

(deftest validate-args-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.core/validate-args))))

(deftest exit-test
  (with-redefs [c/system-exit identity]
    (testing "Calls System/exit with status as an arg"
      (is (= 42 (c/exit 42 "Exit called from tests with status 42."))))))

(with-redefs [sorted.fileio/text-read (constantly test-file)]
  (let [result (c/load-files! ["someFileName"])]
    (deftest load-files!-test
      (testing "Loads expected people into sorted.core/people, ignoring bad records."
        (is (= (butlast test-file) (people->strs @c/people)))))

    (deftest sort-people-test
      (testing "Conforms to spec."
        (is (checks? 'sorted.core/sort-people)))
      (testing "Properly sorts people by date of birth (then last name)"
        (is (= [sam june jane john] (people->strs (c/sort-people ::p/dob)))))
      (testing "Properly sorts by gender"
        (is (= [jane june john sam] (people->strs (c/sort-people ::p/gender)))))
      (testing "Properly sorts by last name (desc)"
        (is (= [sam jane john june] (people->strs (c/sort-people ::p/last-name))))))))

(comment
  (swap! sorted.core/people (fn [_] (vec (map p/str->person (butlast sorted.core-test/test-file)))))
  )
