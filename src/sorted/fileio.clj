(ns sorted.fileio
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [sorted.errors :refer [err-msg]])
  (:import (java.io BufferedReader FileReader)))

(defn exists?
  "Tests if a file exists.
  Returns an ::error if an exception is thrown."
  [file-name]
  (try
    (.exists (io/file file-name))
    (catch Exception e
      (err-msg e "exists?"))))

(defn text-read
  "Returns a vector containing the lines of file-name if it exists.
  The output if file-name is a binary file is undefined.
  Returns an ::error if an exception is thrown."
  [file-name]
  (try
    (with-open [rdr (BufferedReader. (FileReader. file-name))]
      (reduce conj [] (line-seq rdr)))
    (catch Exception e
      (err-msg e "text-read"))))
