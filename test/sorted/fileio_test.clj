(ns sorted.fileio-test
  (:require [clojure.test :refer :all]
            [sorted.fileio :refer :all]
            [failjure.core :as f]
            [clojure.spec.alpha :as s]))

(def ^:private existent-file "README.md")
(def ^:private non-existent-file "non-existent-file.txt")
(def ^:private invalid-file 3)

(deftest exists?-test
  (testing "Checking if a file exists"
    (testing "returns true when the file does exist"
      (is (exists? existent-file)))
    (testing "returns false when the file does not exist"
      (is (not (exists? non-existent-file))))
    (let [error-msg "Error in exists?: No implementation of method: :as-file of protocol: #'clojure.java.io/Coercions found for class: java.lang.Long"
          exists-error "Error in exists?"
          returned (exists? invalid-file)]
      (testing "returns an error when passed an invaldid argument"
        (is (f/failed? returned))
        (testing "with the expected error message text"
          (is (= error-msg (:message returned))))))))

(deftest text-read-test
  (testing "Reading a known file"
    (let [file-lines (text-read existent-file)]
      (testing "returns a vector"
        (is (vector? file-lines))
        (testing "containing only strings"
          (is (every? string? file-lines))))))
  (testing "Reading a non-existent file"
    (let [bad-file non-existent-file
          file-not-found (str "Error in text-read: " non-existent-file " (No such file or directory)")
          file-lines (text-read bad-file)]
      (testing "returns an error"
        (is (f/failed? file-lines))
        (testing "containing a file not found message"
          (is (= file-not-found (f/message file-lines)))))))
  (testing "Reading an invalid file name"
    (let [error-msg "Error in text-read: No matching ctor found for class java.io.FileReader"
          file-lines (text-read invalid-file)]
      (testing "returns an error"
        (is (f/failed? file-lines))
        (testing "containing an exception ojbect"
          (is (= error-msg (f/message file-lines))))))))
