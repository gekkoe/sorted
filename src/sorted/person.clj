(ns sorted.person
  (:require [clojure.spec.alpha :as s]
            [clojure.string :refer [split trim]]
            [clj-time.format :as ctf]
            [failjure.core :as f]))

(defn no-delims?
  "Searches a string for any instance of the delimiters space, comma, or pipe.
  Returns true if none are found or false if one or more is found."
  [s]
  (let [delim-finder #"\|| |,"] ; Finds space, comma, or pipe
    (not (re-find delim-finder s))))

(defn split-trim
  "Splits a string using a delimiter and trims any extra whitespace from the
  front and back of each substring."
  [s delim]
  (map trim (split s delim)))

(defn split-on-delims
  "Attempts to split a string into a vector of strings. Attempts to use pipe,
  comma, or whitespace as delimiters, in that order."
  [s]
  (cond (.contains s "|") (split-trim s #"\|")
        (.contains s ",") (split-trim s #",")
        :else (split-trim s #"\s+")))

(defn strs->vals
  "Attempts to convert a seq of 5 strings to vals that can be converted into a
  person.
  Returns a Failure object if unsuccessful."
  [ss]
  (let [formatter (ctf/formatter "MM/dd/yyyy")]
    (f/try* (conj (vec (drop-last ss))
                  (ctf/parse formatter (last ss))))))

(defn vals->person
  "Given a vector of values, creats a map merging those values with the person
  keywords."
  [vals]
  (zipmap [::first-name ::last-name ::gender ::fav-color ::dob] vals))

(defn str->person
  "Expects a string containing a delimitted list of fields in the order
  LastName FirstName Gender FavoriteColor DateOfBirth
  where valid delimitters are space,comma, or pipe.
  Returns a map that conforms to the :sorted.person/person spec or a Failure
  object."
  [s]
  (f/if-let-ok? [person (f/ok-> s
                                split-on-delims
                                strs->vals
                                vals->person)]
                person
                (f/fail "Error in str->person: Could not parse `%s`\n%s"
                        s
                        (.getMessage person))))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

;; The following spec deliberately allows invalid strings for testing purposes.
;; Validation will occur further through the process of conversion to a person.
(s/def ::maybeperson (s/and string?))

;;; TODO: Add some generators
(s/def ::no-delim-str (s/and string? no-delims?))
(s/def ::last-name ::no-delim-str)
(s/def ::first-name ::no-delim-str)
(s/def ::gender ::no-delim-str)
(s/def ::fav-color ::no-delim-str)
(s/def ::dob inst?)

(s/def ::person (s/keys
                 :req [::first-name ::last-name ::gender ::fav-color ::dob]))

(s/fdef no-delims?
  :args (s/cat :s ::maybeperson)
  :ret boolean?
  :fn (s/or :true (s/and #(= (:ret %) true)
                         #(not (.contains (-> % :args :s) " "))
                         #(not (.contains (-> % :args :s) ","))
                         #(not (.contains (-> % :args :s) "|")))
            :false (s/and #(= (:ret %) false)
                          (s/or :space #(.contains (-> % :args :s) " ")
                                :comma #(.contains (-> % :args :s) ",")
                                :pipe #(.contains (-> % :args :s) "|")))))

(s/fdef str->person
  :args (s/cat :s string?)
  :ret (s/or ::person f/failed?)
  :fn (s/alt ::person (s/and no-delims? #(s/valid? ::person %))
             :message f/failed?))
