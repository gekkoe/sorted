(ns sorted.helpers-test
  (:require [clojure.test :refer :all]
            [sorted.helpers :as h]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]))

(def limited-tests 15) ; These tests will become very slow if > about 25
(def num-tests 100)
(s/def ::non-map (s/and any? (complement map?)))
(s/def ::any any?)

(deftest checks?-test
  (testing "Conforms to spec."
    (is (-> (st/check-fn sorted.helpers/checks?
                         'sorted.helpers/checks?
                         {:clojure.spec.test.check/opts
                          {:num-tests num-tests}})
            :clojure.spec.test.check/ret
            :pass?)))
  (testing "Returns true for known good args."
    (is (h/checks? 'sorted.helpers/checks? num-tests)))
  (testing "Returns logical false when given bad args."
    (is (not (h/checks? 4 5 6)))))

(deftest contains-all?-test
  (testing "Returns true when passed a map and keys it contains"
    (is (contains-all? {:a 1, " " "!", 4 2} :a " " 4)))
  (testing "Returns false when passed a map and keys it doesn't contain"
    (is (not (contains-all? {:a 1 :b 2 :c 3} :d))))
  (testing "Returns false when m is not a map"
    (is (not-any? #(apply contains-all?
                          %
                          (random-sample 0.01 (h/gen-samples ::any num-tests)))
                  (h/gen-samples ::non-map num-tests)))))

(deftest gen-samples-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.helpers/gen-samples limited-tests))))

(deftest verified?-test
  (testing "Conforms to spec."
    (is (h/checks? 'sorted.helpers/verified? limited-tests))))
