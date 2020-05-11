(ns sorted.core-test
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [join]]
            [clojure.test :refer :all]
            [sorted.core :as c]
            [sorted.helpers :as h]
            [sorted.people :as ppl]
            [sorted.person :as p]))

(def num-tests 1000) ; Increase this to run slower but more exhaustive tests.
(def checks? (h/checks? num-tests))
(def num-samples 1000)
(def samples (h/gen-samples num-samples))

(def p1 "Doe John Male Green 1/1/1995")
(def p2 "Doe Jane Female Blue 2/9/1993")
(def p3 "Brin Frin Female Orange 2/9/1993")
(def by-dob (join \newline [p3 p2 p1]))
(def unsorted-strs (join \newline [p1 p2 p3]))
(def person1 (p/str->person p1))
(def person2 (p/str->person p2))
(def person3 (p/str->person p3))
(def unsorted-ps [person1 person2 person3])

(def usage-summary
  (str "sorted - A simple program for sorting people.\n\nUsage: sorted "
       "[options] file1 file2 file3 ...\n\nOptions:\n  -d, --dob       "
       " Sort output by date of birth, ascending.\n  -g, --gender     "
       "Sort by gender, female first, then by last name ascending.\n  "
       "-h, --help       Prints this help text...\n  -l, --last       "
       "Sort output by last name, descending.\n  -p, --port PORT  Port number. "
       "If not specified, doesn't start server.\n\nThis program can support "
       "more than 3 files if desired, but at least one must be provided.\n"))

(defn usage? [r] (= usage-summary (::c/exit-msg r)))
(defn exit-inst? [r] (s/valid? ::c/exit-instructions r))
(defn actions? [r] (s/valid? ::c/actions r))

(s/def ::non-option (s/and string? #(not (.contains ^String % "-"))))
(s/def ::args (s/* string?))

(defn help-args
  "Generate arg collection containing at least one -h flag."
  []
  (map shuffle
       (map #(conj % "-h")
            (samples ::args))))

(deftest usage-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.core/usage))))

(deftest error-msg-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.core/error-msg))))

(deftest validate-args-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.core/validate-args)))

  (testing (str "When help flag passed, returns exit instructions "
                "regardless of other args")
    (is (every? true? (map exit-inst? (map c/validate-args (help-args)))))
    (let [result (c/validate-args ["--help"])]
      (testing "which contains the expected message"
        (is (usage? result)))
      (testing "which contains a :sorted.core/ok? key with the value true"
        (is (= true (::c/ok? result))))))

  (testing (str "When no help flag is passed but passed bad args, returns"
                "exit instructions")
    (let [result (c/validate-args ["-NotGood"])]
      (is (exit-inst? result))
      (testing "which contains the expected message"
        (is (= (str "The following errors occurred while parsing your "
                    "command:\n\nUnknown option: \"-N\"\nUnknown option: "
                    "\"-o\"\nUnknown option: \"-t\"\nUnknown option: "
                    "\"-G\"\nUnknown option: \"-o\"\nUnknown option: "
                    "\"-o\"\n\n"
                    usage-summary) (::c/exit-msg result))))))

  (testing "When given too many sort options, returns exit instructions"
    (let [result (c/validate-args ["-d" "--last" "somefile" "anotherfile"])]
      (is (exit-inst? result))
      (testing "containing the expected message"
        (is (= "Please select no more than one sort option."
               (::c/exit-msg result))))))

  (testing "When no options or files are passed, returns an exit message"
    (let [result (c/validate-args [])]
      (is (s/valid? ::c/exit-instructions result))
      (testing "which contains the expected message"
        (is (usage? result)))))

  (testing "When passed non-help options but no files, returns an exit message"
    (let [result (c/validate-args ["-d" "-p2000"])]
      (is (exit-inst? result))
      (testing "containing the expected message"
        (is (= "Please provide at least one file with person records in it."
               (::c/exit-msg result))))))

  (testing (str "When passed a port, file(s), no errors, and no other options "
                "returns :sorted.core/actions")
    (let [result (c/validate-args ["-p2000" "somefile" "anotherfile"])]
      (is (actions? result))
      (testing "containing the expected :sorted.core/port"
        (is (= 2000 (::c/port result))))
      (testing "containing the expected file(s)"
        (is (= ["somefile" "anotherfile"] (::c/files result))))))

  (testing (str "When passed zero or more valid options, one or more file(s), "
                "no errors, no help flag, and no port, returns a"
                ":sorted.core/actions")
    (let [result (c/validate-args ["--gender" "somefile"])]
      (is (actions? result))
      (testing "containing the expected file(s)"
        (is (= ["somefile"] (::c/files result))))
      (testing "containing the expected sort-kw"
        (is (= ::p/gender (::c/sort-kw result)))))))

(deftest sorted-people-str-test
  (testing "Conforms to spec"
    (checks? 'sorted.core/sorted-people-str))
  (with-redefs [ppl/people (atom unsorted-ps)]
    (testing "Returns the expected string when passed a valid sort-kw"
      (is (= by-dob (c/sorted-people-str ::p/dob))))
    (testing "Returns unsorted list when passed invalid sort-kw"
      (is (= unsorted-strs (c/sorted-people-str ::made-up-kw))))))
