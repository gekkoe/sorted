(ns sorted.handler-test
  (:require [clojure.test :refer :all]
            [sorted.handler :as handler]
            [sorted.helpers :as h]
            [sorted.people :as ppl]
            [sorted.person :as p]
            [clojure.spec.alpha :as s]))

(def num-tests 1000)    ; Number of times to check function specs.
(def checks? (h/checks? num-tests))
(def num-samples 1000)
(def samples (h/gen-samples num-samples))

(def p1 "Doe,John,Male,Green,1/1/1995")
(def p2 "Doe,Jane,Female,Blue,2/9/1993")
(def person1 (p/str->person p1))
(def person2 (p/str->person p2))
(def test-ctx {:request {:headers {"content-type" :test}
                         :request-method :post}})

(s/def ::non-post (s/and simple-keyword? #(not= :post %)))
(s/def ::any any?)
(def non-post-ctx-list (map (fn [x] {:request {:request-method x}}) (samples ::non-post)))
(def post-ctx {:request {:request-method :post}})

(deftest strs->html-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.handler/strs->html)))
  (testing "Returns expected html"
    (is (= "<div><p>1</p><p>2</p><p>3</p></div>"
           (handler/strs->html ["1" "2" "3"])))))

(deftest link-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.handler/link)))
  (testing "Returns expected html"
    (is (= "<a href=\"/test\">/test</a>" (handler/link "/test")))))

(deftest already-exists?-test
  (with-redefs [ppl/people (atom [person1])]
    (testing "Conforms to spec."
      (is (checks? 'sorted.handler/already-exists?)))
    (testing "Returns expected vector when sorted.people/people contains a dup"
      (is (= [true {:message "Record for this person already exists."}]
             (handler/already-exists? {::handler/person person1}))))
    (testing "Returns logical false when sorted.people/people has no dup"
      (is (not (handler/already-exists? {::handler/person person2}))))))

(deftest posting?-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.handler/posting?)))
  (testing "Returns true if request method is :post"
    (is (handler/posting? post-ctx)))
  (testing "Returns logical false if request method is anything but post"
    (is (not-any? true? (map handler/posting? non-post-ctx-list)))))

(deftest check-content-type-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.handler/check-content-type)))
  (testing "Returns true if request method is not post regardless of content-types"
    (is (every? true? (map handler/check-content-type
                           non-post-ctx-list
                           (samples ::handler/content-types)))))
  (testing "Returns true if request method is post and content-types "))

(deftest parse-person-str-test
  (with-redefs [ppl/people (atom [person1 person2])]
    (testing "Conforms to spec."
      (is (checks? 'sorted.handler/parse-person-str)))))

(deftest post-person!-test
    (testing "Conforms to spec."
      (is
       (with-redefs [ppl/people (atom [person1 person2])]
       (checks? 'sorted.handler/post-person!)))))

(deftest sorted-people-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.handler/sorted-people)))
  (testing "Produces expected results when passed good args"
    (with-redefs [ppl/people (atom [person1 person2])]
      (is (= [(p/person->un-person person2)
              (p/person->un-person person1)]
             (-> (handler/sorted-people ::p/gender)
                 ::handler/un-people
                 :records))))))
