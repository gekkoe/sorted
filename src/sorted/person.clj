(ns sorted.person
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [split trim join]]
            [failjure.core :as f]
            [java-time :as jt]))

;;;============================================================================
;;;                          H E L P E R S
;;;============================================================================

;; Regex that will find pipe, space, or comma
(def delims #"\||,| ")

(def formatter (jt/formatter "MM/dd/yyyy"))

(defn date->str
  "Expects a map conforming to sorted.person/date. Converts it to a string in
  the formate MM/dd/yyyy.
  Returns a Failure object if unsuccessful."
  [d]
  (if (s/valid? ::date d)
    (format "%02d/%02d/%04d" (::month d) (::day d) (::year d))
    (f/fail "Error in date->str. Passed an invalid date. `%s`" d)))

(defn epoch->str
  "Expects a number representing days from the epoch date (1/1/1970) and
  produces a string based on that date using the format MM/dd/yyyy
  Returns a Failure object if unsuccessful."
  [d]
  (f/if-let-ok?
   [date (f/try* (jt/format formatter (. java.time.LocalDate ofEpochDay d)))]
   date
   (f/fail "Error in epoch->str: Could not parse `%s`\n%s" d (f/message date))))

(defn no-delims?
  "Searches a string for any instance of the delimiters space, comma, or pipe.
  Returns true if none are found or false if one or more is found."
  [s]
  (not (re-find delims s)))

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
        :else (split-trim s #" "))) ; TODO: should this be \s+ or...?

(defn str->date
  "Expects a string date formatted as MM/dd/yyyy and converts it to a map
  conforming to sorted.person/date.
  Returns a Failure object if unsuccessful."
  [s]
  (if (s/valid? ::datestr s)
    (as-> s d
      (split d #"/")
      (map #(Integer. %) d)
      (zipmap [::month ::day ::year] d))
    (f/fail "Error in str->date. Passed an invalid date. `%s`" s)))

(defn strs->vals
  "Attempts to convert a seq of 5 strings to vals that can be converted into a
  person.
  Returns a Failure object if unsuccessful."
  [ss]
  (f/try* (conj (vec (drop-last ss))
                (str->date (last ss)))))

(defn vals->person
  "Given a vector of values, creats a map merging those values with the person
  keywords."
  [vals]
  (zipmap [::last-name ::first-name ::gender ::fav-color ::dob] vals))

;;;============================================================================
;;;                           P U B L I C
;;;============================================================================

(defn str->person
  "Expects a string containing a delimited list of fields in the order
  LastName FirstName Gender FavoriteColor DateOfBirth
  where valid delims are space, comma, or pipe and date format is MM/dd/yyyy.
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
  Returns a string representation of those values delimited by delim."
  [{:keys [::last-name ::first-name ::gender ::fav-color ::dob]} delim]
  (let [first4 (join delim [last-name first-name gender fav-color])
        dob-str (date->str dob)]
    (join delim [first4 dob-str])))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(s/def ::delim (s/with-gen string? #(s/gen #{" " "," "|"})))

;; Simple representation of a date. For production code that did more than just
;; sort we'd likely want to use java.time.LocalDate or something similar.
(s/def ::year (s/int-in 1900 2101))
(s/def ::month (s/int-in 1 13))
(s/def ::day (s/int-in 1 32))
(s/def ::date (s/keys :req [::year ::month ::day]))

;; Generate strings of dates in a desired range for generative tests.
(def min-day -25567) ; 01/01/1900
(def max-day  47845) ; 12/30/2100
(s/def ::datestr
  (s/with-gen #(f/ok? (f/try* (jt/local-date formatter %)))
    #(gen/fmap epoch->str (gen/choose min-day max-day))))

(s/def ::no-delim-str (s/and string? no-delims?))
(s/def ::last-name ::no-delim-str)
(s/def ::first-name ::no-delim-str)
(s/def ::gender ::no-delim-str)
(s/def ::fav-color ::no-delim-str)
(s/def ::dob ::date)

(s/def ::person (s/keys :req [::first-name ::last-name ::gender ::fav-color ::dob]))

;; NOTE: This generator is rather specific, so it has a lowered likelihood of
;; working. So it requires an override to max-tries. Without it, test.check will
;; only attempt it 10 times, and will generally fail on string or string-ascii.
;; It does work for string-alphanumeric though.
(def non-delim (gen/such-that no-delims? (gen/string) 100))

;; The following spec deliberately allows invalid strings for testing purposes.
;; Validation will occur further through the process of conversion to a ::person.
(s/def ::maybe-person-str string?)

;; Some regex matchers that will determine if a string appears to be a
;; well-formed delimited string that could represent a ::person
(def non-ws-delims #".*?([\|,]).*?\1.*?\1.*?\1\s*\d{1,2}/\d{1,2}/\d{1,4}\s*$")
(def ws-delims     #".*(\s+).*\s+.*\s+.*\s+\d+/\d+/\d+\s*$")

(s/def ::person-str
  (s/with-gen #(or (re-matches non-ws-delims %) (re-matches ws-delims %))
    #(gen/fmap (fn [[last first gender color dob delim]]
                 (join delim [last first gender color (epoch->str dob)]))
               (gen/tuple non-delim                                  ; Last
                          non-delim                                  ; First
                          non-delim                                  ; Gender
                          non-delim                                  ; Color
                          (gen/choose min-day max-day)               ; DoB
                          (gen/elements [" " "|" ","])))))           ; Delim

(s/fdef no-delims?
  :args (s/cat :s ::maybe-person-str)
  :ret boolean?
  :fn (let [ds [#" " #"," #"\|"]
            found-delim? (fn [result delim] (re-find delim (-> result :args :s)))]
        (s/or :true (s/and #(:ret %)
                           ;; No delims found in the original string.
                           (fn [result] (not-any? #(found-delim? result %) ds)))
              :false (s/and #(not (:ret %))
                            ;; At least one delim found in original string.
                            (fn [result] (some #(found-delim? result %) ds))))))

(s/fdef str->person
  :args (s/cat :s ::person-str)
  :ret  (s/or :success ::person
              :failure f/failed?))

(s/fdef person->str
  :args (s/cat :p ::person :d ::delim)
  :ret  (s/or :success ::person-str
              :failure f/failed?)
  ;; Verify that we can turn the ret val back into the original ::person
  :fn (fn [r] (= (str->person (-> r :ret second))
                 (-> r :args :p))))

(comment
  (expound/explain-results (st/check 'sorted.person/person->str))

  (expound/explain-results (st/check 'sorted.person/no-delims?
                                     {:clojure.spec.test.check/opts
                                      {:num-tests 1000}}))
  )
