(ns sorted.person
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [split trim join blank?]]
            [clj-time.format :as ctf]
            [clj-time.coerce :as ctc]
            [failjure.core :as f]))

;; DateTime formatter for clj-time operations
(def formatter (ctf/formatter "MM/dd/yyyy"))

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
  (f/try* (conj (vec (drop-last ss))
                (ctf/parse formatter (last ss)))))

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
                        (f/message person))))

(defn person->str
  "Expects a map that conforms to :sorted.person/person.
  Returns a string representation of those values delimitted by d."
  [{:keys [::last-name ::first-name ::gender ::fav-color ::dob]} d]
  (let [first4 (join d [last-name first-name gender fav-color])
        ;; TODO: Write a generator to produce org.joda.time.ReadableInstant
        ;;  in ::dob so that this coercion isn't necessary for s/exercise-fn
        coerced-dob (if (= java.util.Date (type dob)) (ctc/from-date dob) dob)
        dob-str (ctf/unparse formatter coerced-dob)]
    (join d [first4 dob-str])))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

;; The following spec deliberately allows invalid strings for testing purposes.
;; Validation will occur further through the process of conversion to a person.
(s/def ::maybeperson string?)

;; Arbitrary long ints to represent some random dates
(def begin-date 500000000000)
(def end-date 5000000000000)

(s/def ::date
  (s/with-gen #(instance? java.util.Date %)
    (fn [] (gen/fmap #(-> % ctc/to-date)
                     (s/gen (s/int-in begin-date end-date))))))

;;; TODO: Add some generators
(s/def ::no-delim-str (s/and string? (complement blank?) no-delims?))
(s/def ::last-name ::no-delim-str)
(s/def ::first-name ::no-delim-str)
(s/def ::gender ::no-delim-str)
(s/def ::fav-color ::no-delim-str)
(s/def ::dob ::date)

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
  :ret (s/alt ::person f/failed?)
  :fn (s/alt ::person (s/and no-delims? #(s/valid? ::person %))
             :message f/failed?))

(s/fdef person->str
  :args (s/cat :p ::person :d #{" " "," "|"})
  :ret (s/or ::maybeperson string?
             :error f/failed?)
  ;; Verify that we can turn the ret val back into the original ::person
  :fn (fn [r] (= (str->person (second (:ret r))) (-> r :args :p))))

(comment
  (expound/explain-result (st/check-fn sorted.person/person->str 'sorted.person/person->str))
  )
