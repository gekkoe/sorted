(ns sorted.core-test
  (:require [clojure.test :refer :all]
            [sorted.core :as c]
            [sorted.helpers :as h]
            [sorted.people :as ppl]
            [sorted.person :as p]))

(def num-tests 1000) ; Increase this to run slower but more exhaustive tests.
(def checks? (h/checks? num-tests))

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
