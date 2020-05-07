(ns sorted.people-test
  (:require [clojure.test :refer :all]
            [sorted.helpers :as h]
            [sorted.people :as ppl]
            [sorted.person :as p]))

(def num-tests 1000) ; Increase this to run slower but more exhaustive tests.
(def checks? (h/checks? num-tests))

(def jane "Doe Jane Female Purple 1/1/2000")
(def june "Darla June Female Orange 1/1/2000")
(def john "Dean John Male Blue 12/31/2001")
(def sam  "Francis Sam Male Green 2/11/1994")
(def malformed "Some Invalid Record")
(def test-file [jane june john sam malformed])

(with-redefs [ppl/people (atom (map p/str->person test-file))]
  (deftest sorted-by-test
    (testing "Conforms to spec."
      (is (checks? 'sorted.people/sorted-by)))
    (testing "Properly sorts people by date of birth (then last name)"
      (is (= [sam june jane john] (ppl/people->strs (ppl/sorted-by ::p/dob)))))
    (testing "Properly sorts by gender"
      (is (= [jane june john sam] (ppl/people->strs (ppl/sorted-by ::p/gender)))))
    (testing "Properly sorts by last name (desc)"
      (is (= [sam jane john june] (ppl/people->strs (ppl/sorted-by ::p/last-name)))))))
