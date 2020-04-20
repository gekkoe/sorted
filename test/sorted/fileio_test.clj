(ns sorted.fileio-test
  (:require [clojure.test :refer :all]
            [sorted.fileio :refer :all]
            [sorted.errors :as err]
            [clojure.spec.alpha :as s]))

(deftest exists?-test
  (let [existant-file "README.md"
        non-existant-file "non-existant-file.txt"
        invalid-file 3]
    (testing "Checking if a file exists"
      (testing "returns true when the file does exist"
        (is (exists? existant-file)))
      (testing "returns false when the file does not exist"
        (is (not (exists? non-existant-file))))
      (let [exists-error "Error in exists?"
            ex (exists? invalid-file)]
        (testing "returns an error when passed an invaldid argument"
          (is (s/valid? ::err/error ex))
          (testing "beginning with the expected error text"
            (is (= exists-error (subs (::err/message ex) 0 (count exists-error))))))))))

(deftest text-read-test
  (testing "Reading a known file"
    (let [file (text-read existant-file)]
      (testing "returns a vector"
        (is (vector? file))
        (testing "containing only strings"
          (is (every? string? file))))))
  (testing "Reading a non-existant file."
    (let [bad-file non-existant-file
          file-not-found (str "Error in text-read: " non-existant-file " (No such file or directory)")
          file (text-read bad-file)]
      (testing "Returns an error"
        (is (s/valid? ::err/error file))
        (testing "containing a file not found message"
          (is (= file-not-found (::err/message file)))))))
  (testing "Reading an invalid file name"
    (let [invalid-error "Error in text-read: No matching ctor found for class java.io.FileReader"
          file (text-read invalid-file)]
      (testing "returns an error"
        (is (s/valid? ::err/error file))
        (testing "containing the expected error text"
          (is (= invalid-error (::err/message file))))))))
