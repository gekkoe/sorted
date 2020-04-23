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
  (let [raw-vals (cond (.contains s "|") (split-trim s #"|")
                       (.contains s ",") (split-trim s #",")
                       :else (split-trim s #"\s+"))
        ;; TODO: Put failjure try clause in here for exceptoins thrown by
        ;;       clj-time on bad dates.
        vals (conj (vec (drop-last raw-vals)) (ctf/parse (last raw-vals)))
        person (vals->person vals)]
    (if (s/valid? ::person person)
      person
      (f/fail "Error in str->person: Could not parse \"%s\"" s))))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================
(s/def ::raw (s/and string? (complement no-delims?))) ; Unparsed str of a person

;;; TODO: Add some generators
(s/def ::last-name (s/and string? no-delims?))
(s/def ::first-name (s/and string? no-delims?))
(s/def ::gender (s/and string? no-delims?))
(s/def ::fav-color (s/and string? no-delims?))
(s/def ::dob inst?)

(s/def ::person (s/keys
                 :req [::first-name ::last-name ::gender ::fav-color ::dob]))

(s/fdef no-delims?
  :args (s/cat :s ::raw)
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
