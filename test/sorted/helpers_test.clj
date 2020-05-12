(ns sorted.helpers-test
  (:require [clojure.test :refer :all]
            [sorted.helpers :as h]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]))

(def num-tests 1000)
(def checks? (h/checks? num-tests))
(def num-samples 250)
(s/def ::non-map (s/and any? (complement map?)))
(s/def ::any any?)

(deftest any-or-test
  (testing "Conforms to spec"
    (is (checks? 'sorted.helpers/any-or))))

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
    (is (h/contains-all? {:a 1, " " "!", 4 2} :a " " 4)))
  (testing "Returns false when passed a map and keys it doesn't contain"
    (is (not (h/contains-all? {:a 1 :b 2 :c 3} :d))))
  (testing "Returns false when m is not a map"
    (is (not-any? #(apply h/contains-all?
                          %
                          (random-sample 0.01 (h/gen-samples ::any num-samples)))
                  (h/gen-samples ::non-map num-samples)))))

(deftest gen-samples-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.helpers/gen-samples))))

(deftest get-free-port-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.helpers/get-free-port))))

(deftest ok-map-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.helpers/ok-map))))

(deftest verified?-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.helpers/verified?))))
