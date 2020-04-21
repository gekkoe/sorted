(ns sorted.fileio
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [failjure.core :as f])
  (:import (java.io BufferedReader FileReader)))

(defn exists?
  "Tests if a file exists.
  Returns an Failure object if an exception is thrown."
  [file-name]
  (try
    (.exists (io/file file-name))
    (catch Exception e
      (f/fail "Error in exists?: %s" (.getMessage e)))))

(defn text-read
  "Returns a vector containing the lines of file-name if it exists.
  The output if file-name is a binary file is undefined.
  Returns a Failure object if an exception is thrown."
  [file-name]
  (try
    (with-open [rdr (BufferedReader. (FileReader. file-name))]
      (reduce conj [] (line-seq rdr)))
    (catch Exception e
      (f/fail "Error in text-read: %s" (.getMessage e)))))
