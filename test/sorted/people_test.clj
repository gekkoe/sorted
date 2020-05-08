(ns sorted.people-test
  (:require [clojure.test :refer :all]
            [sorted.helpers :as h]
            [sorted.people :as ppl]
            [sorted.person :as p]
            [sorted.fileio :as file]))

(def num-tests 1000) ; Increase this to run slower but more exhaustive tests.
(def checks? (h/checks? num-tests))

(def jane "Doe Jane Female Purple 1/1/2000")
(def june "Darla June Female Orange 1/1/2000")
(def john "Dean John Male Blue 12/31/2001")
(def sam  "Francis Sam Male Green 2/11/1994")
(def malformed "Some Invalid Record")
(def test-file [jane june john sam malformed])

(deftest load-from-files!-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.people/load-from-files!)))
  (let [result (with-redefs [file/text-read (constantly test-file)
                             ppl/people     (atom [])]
                 (ppl/load-from-files! ["someFileName"]))]
    (testing "Loads expected people, ignoring bad records."
      (is (= (butlast test-file) (ppl/people->strs result))))))

  (deftest sorted-by-test
      (testing "Conforms to spec."
        (is (checks? 'sorted.people/sorted-by)))
    (with-redefs [ppl/people (atom (mapv p/str->person (butlast test-file)))]
      (testing "Properly sorts people by date of birth (then last name)"
        (is (= [sam june jane john] (ppl/people->strs (ppl/sorted-by ::p/dob)))))
      (testing "Properly sorts by gender"
        (is (= [jane june john sam] (ppl/people->strs (ppl/sorted-by ::p/gender)))))
      (testing "Properly sorts by last name (desc)"
        (is (= [sam jane john june] (ppl/people->strs (ppl/sorted-by ::p/last-name)))))))
