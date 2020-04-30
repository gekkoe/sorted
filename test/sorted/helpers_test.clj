(ns sorted.helpers-test
  (:require [clojure.test :refer :all]
            [sorted.helpers :as h]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]))

(def num-tests 15) ; These tests will become very slow if > about 25

(deftest checks?-test
  (testing "Conforms to spec."
    (is (-> (st/check-fn sorted.helpers/checks?
                         'sorted.helpers/checks?
                         {:clojure.spec.test.check/opts
                          {:num-tests num-tests}})
            :clojure.spec.test.check/ret
            :pass?)))
  (testing "Returns true for known good args."
    (is (h/checks? sorted.helpers/checks?
                   'sorted.helpers/checks?
                   num-tests)))
  (testing "Returns falsy value when given bad args."
    (is (not (h/checks? 4 5 6)))))

(deftest gen-samples-test
  (testing "Conforms to spec."
    (is (h/checks? sorted.helpers/gen-samples
                   'sorted.helpers/gen-samples
                   num-tests))))

(deftest verified?-test
  (testing "Conforms to spec."
    (is (h/checks? sorted.helpers/verified?
                   'sorted.helpers/verified?
                   num-tests))))
