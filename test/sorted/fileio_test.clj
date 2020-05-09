(ns sorted.fileio-test
  (:require [clojure.test :refer :all]
            [sorted.fileio :as file]
            [failjure.core :as f]
            [sorted.helpers :as h]))

(def existent-file "README.md")
(def non-existent-file "non-existent-file.txt")
(def invalid-file 3)
(def num-tests 500)
(def checks? (h/checks? num-tests))

(deftest exists?-test
  (testing "Conforms to spec."
    (checks? 'sorted.fileio/exists?))
  (testing "Checking if a file exists"
    (testing "returns true when the file does exist"
      (is (file/exists? existent-file)))
    (testing "returns false when the file does not exist"
      (is (not (file/exists? non-existent-file))))
    (let [error-msg (str "Error in exists?: No implementation of method: "
                         ":as-file of protocol: #'clojure.java.io/Coercions "
                         "found for class: java.lang.Long")
          returned (file/exists? invalid-file)]
      (testing "returns an error when passed an invaldid argument"
        (is (f/failed? returned))
        (testing "with the expected error message text"
          (is (= error-msg (f/message returned))))))))

(deftest text-read-test
  (testing "Conforms to spec."
    (is (checks? 'sorted.fileio/text-read)))
  (testing "Reading a known file"
    (let [file-lines (file/text-read existent-file)]
      (testing "returns a vector"
        (is (vector? file-lines))
        (testing "containing only strings"
          (is (every? string? file-lines))))))
  (testing "Reading a non-existent file"
    (let [bad-file non-existent-file
          file-not-found (str "Error in text-read: "
                              non-existent-file
                              " (No such file or directory)")
          file-lines (file/text-read bad-file)]
      (testing "returns an error"
        (is (f/failed? file-lines))
        (testing "containing a file not found message"
          (is (= file-not-found (f/message file-lines)))))))
  (testing "Reading an invalid file name"
    (let [error-msg (str "Error in text-read: java.lang.Long cannot be "
                         "cast to java.lang.String")
          file-lines (file/text-read invalid-file)]
      (testing "returns an error"
        (is (f/failed? file-lines))
        (testing "containing an exception message"
          (is (= error-msg (f/message file-lines))))))))
