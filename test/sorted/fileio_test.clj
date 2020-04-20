(ns sorted.fileio-test
  (:require [clojure.test :refer :all]
            [sorted.fileio :refer :all]
            [sorted.errors :as err]
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
    (let [exists-error "Error in exists?"
          ex (exists? invalid-file)]
      (testing "returns an error when passed an invaldid argument"
        (is (s/valid? ::err/error ex))
        (testing "beginning with the expected error text"
          (is (= exists-error (subs (::err/message ex) 0 (count exists-error)))))))))

(deftest text-read-test
  (testing "Reading a known file"
    (let [file (text-read existent-file)]
      (testing "returns a vector"
        (is (vector? file))
        (testing "containing only strings"
          (is (every? string? file))))))
  (testing "Reading a non-existent file"
    (let [bad-file non-existent-file
          file-not-found (str "Error in text-read: " non-existent-file " (No such file or directory)")
          file (text-read bad-file)]
      (testing "returns an error"
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