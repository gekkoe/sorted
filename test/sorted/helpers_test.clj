(ns sorted.helpers-test
  (:require [clojure.test :refer :all]
            [sorted.helpers :as h]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]))

(deftest gen-samples-test
  (testing "Conforms to spec."
    (is (-> (st/check-fn sorted.helpers/gen-samples 'sorted.helpers/gen-samples
                         {:clojure.spec.test.check/opts {:num-tests 20}})
            :clojure.spec.test.check/ret
            :pass?))))
