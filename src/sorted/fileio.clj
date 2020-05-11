(ns sorted.fileio
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io]
            [failjure.core :as f])
  (:import (java.io BufferedReader FileReader)))

;;;============================================================================
;;;                              P U B L I C
;;;============================================================================

(defn exists?
  "Tests if a file exists.
  Returns an Failure object if an exception is thrown."
  [file-name]
  (f/if-let-ok? [exists? (f/try* (.exists ^java.io.File (io/file file-name)))]
                exists?
                (f/fail "Error in exists?: %s" (f/message exists?))))

(defn text-read
  "Returns a vector containing the lines of file-name if it exists.
  The output, if file-name is a binary file, is undefined.
  Returns a Failure object if an exception is thrown."
  [file-name]
  (f/if-let-ok? [text (f/try* (with-open
                                [rdr (BufferedReader. (FileReader. ^String file-name))]
                                (vec (line-seq rdr))))]
                text
                (f/fail "Error in text-read: %s" (f/message text))))

;;;============================================================================
;;;                        S P E C    R E L A T E D
;;;============================================================================

(s/fdef exists?
  :args (s/cat :file-name string?)
  :ret (s/or :success boolean?
             :failure f/failed?))

(s/fdef text-read
  :args (s/cat :file-name string?)
  :ret (s/or :success (s/coll-of string? :into [])
             :failure f/failed?))
