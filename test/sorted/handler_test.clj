(ns sorted.handler-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test :refer :all]
            [sorted.handler :as handler]
            [sorted.helpers :as h]
            [sorted.people :as ppl]
            [sorted.person :as p]))

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
(def valid-content-types (into [] handler/content-type-set))

(s/def ::non-post (s/and simple-keyword? #(not= :post %)))
(s/def ::any any?)
(def post-ctx {:request {:headers {"content-type" ""} :request-method :post}})
(def non-post-ctx-list (map #(assoc-in post-ctx [:request :request-method] %)
                            (samples ::non-post)))
(defn make-post [content-type] (assoc-in
                                post-ctx
                                [:request :headers "content-type"]
                                content-type))
(def post-ctx-list (map make-post (gen/sample
                                   handler/content-type-gen
                                   num-samples)))

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
  (testing (str "Returns true if request method is not post regardless of "
                "content-types")
    (is (every? true? (map handler/check-content-type
                           non-post-ctx-list
                           (samples ::handler/content-types)))))
  (testing (str "Returns true if a post ctx has a content type matching one in "
                "content-types")
    (is (every? true? (map #(handler/check-content-type % valid-content-types)
                           post-ctx-list))))
  (testing (str "Returns a ::handler/false-msg if a post ctx does not have a "
                "content type matching one in content-types")
    (is (every? #(s/valid? ::handler/false-msg %)
                (map #(handler/check-content-type % ["unknown" "types"])
                     post-ctx-list)))))

(deftest check-people-count-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.handler/check-people-count)))
  (testing (str "Returns true if request method is not post")
    (is (every? true? (map handler/check-people-count non-post-ctx-list))))
  (testing (str "Returns true if ctx is a post request and post limit is not "
                "exceeded")
    (with-redefs [ppl/people (atom [])]
      (is (every? true? (map handler/check-people-count post-ctx-list)))))
  (testing (str "Returns a ::handler/false-map if ctx is a post request and the "
                "post limit is exceeded")
    (with-redefs [ppl/people (atom (range ppl/post-limit))]
      (is (every? #(s/valid? ::handler/false-msg %)
                  (map handler/check-people-count post-ctx-list))))))

(deftest parse-person-str-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.handler/parse-person-str)))
  (testing "Returns logical false if request method is not post"
    (is (not-any? true? (map handler/parse-person-str non-post-ctx-list))))
  (let [p (gen/generate (s/gen ::handler/person-ctx))
        p-post (assoc-in p [:request :request-method] :post)
        bad-p "not a person string"
        bad-p-post (assoc-in p-post [:request :body] (handler/make-stream bad-p))
        result (handler/parse-person-str p-post)
        bad-result (handler/parse-person-str bad-p-post)]
    (testing (str "If ctx is a post request and contains a valid ::p/person-str "
                  "in the body, returns a ::handler/false-map")
      (is (s/valid? ::handler/false-map result))
      (testing "containing a valid ::p/person in its ::handler/person key"
        (is (s/valid? ::p/person (::handler/person (second result))))))
    (testing (str "If ctx is a post request and contains an invalid "
                  "::p/person-str in the body, returns a ::handler/msg-map")
      (is (s/valid? ::handler/msg-map bad-result)))))

(deftest post-person!-test
  (with-redefs [ppl/people (atom [])]
    (testing "Conforms to spec."
      (is (checks? 'sorted.handler/post-person!))))
  (let [p (gen/generate (s/gen ::p/person))
        ;; Make a person with an impossible name, so it's guaranteed unique
        diff-p (assoc p
                ::p/last-name
                (gen/generate (s/gen ::p/delim-str)))
        ctx (gen/generate (s/gen ::handler/ctx))
        insert-p #(assoc ctx ::handler/person %)
        p-ctx (insert-p p)
        diff-p-ctx (insert-p diff-p)]

    (with-redefs [ppl/people (atom [p])]
      (testing "If a person already exists in the collection"
        (let [result (handler/post-person! p-ctx)
              result-un-p (-> result :message :added)]
          (testing "the collection will remain unchanged"
            (is (= [p] @ppl/people)))
          (testing "returns a ::handler/msg-map"
            (is (s/valid? ::handler/msg-map result))
            (testing "containing a ::p/un-person version of the ctx person"
              (is (= (p/person->un-person p) result-un-p)))))))

    (with-redefs [ppl/people (atom [diff-p])]
      (testing "If a person does not already exist in the collection"
        (let [result (handler/post-person! p-ctx)
              result-un-p (-> result :message :added)]
          (testing "the collection will now contain the person"
            (is (= [diff-p p] @ppl/people)))
          (testing "returns a ::handler/msg-map"
            (is (s/valid? ::handler/msg-map result))
            (testing "containing a ::p/un-person version of the ctx person"
              (is (= (p/person->un-person p) result-un-p)))))))))

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
