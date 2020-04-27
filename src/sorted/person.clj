(ns sorted.person
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [split trim join]]
            [failjure.core :as f]
            [java-time :as jt]))

;;;============================================================================
;;;                          H E L P E R S
;;;============================================================================

;; Regex that will find pipe, comma, or space
;; NOTE: If these change, it'll be necessary to update doc strings throughout
;; this file.
(def delims #"\||,| ")

(def formatter (jt/formatter "MM/dd/yyyy"))

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
  "Searches a string for any instance of the delimiters pipe, comma, or space.
  Returns true if none are found or false if one or more is found."
  [s]
  (not (re-find delims s)))

(defn split-trim
  "Splits a string using a delimiter and trims any extra whitespace from the
  front and back of each substring.
  Returns a Failure object if unsuccessful."
  [s delim]
  (if (and (s/valid? ::person-str s)
           (s/valid? ::delim-regex delim))
    (map trim (split s delim))
    (f/fail "Error in split-trim: Could not split person `%s` with delim `%s`"
            s delim)))

(defn split-on-delims
  "Attempts to split a string into a vector of strings. Attempts to use pipe,
  comma, or space as delimiters, in that order.
  Returns a Failure object if unsuccessful."
  [s]
  (f/if-let-ok? [result (f/try*
                         (cond (re-find #"\|" s) (split-trim s #"\|")
                               (re-find #"," s) (split-trim s #",")
                               :else (split-trim s #" ")))]
                result
                (f/fail "Error in split-on-delims: Could not parse `%s`\n%s"
                        s (f/message result))))

(defn strs->vals
  "Attempts to convert a seq of 5 strings to vals that can be converted into a
  person.
  Returns a Failure object if unsuccessful."
  [ss]
  (f/if-let-ok? [date (f/try* (jt/local-date formatter (last ss)))]
                (conj (vec (drop-last ss)) date)
                (f/fail "Error in str->vals: Could not parse `%s`\n%s"
                        ss (f/message date))))

(defn vals->person
  "Given a vector of vals such as the output of strs->vals, creates a map
  conforming to sorted.person/person."
  [vals]
  ;; TODO: Put some error handling in here.
  (zipmap [::last-name ::first-name ::gender ::fav-color ::dob] vals))

;;;============================================================================
;;;                           P U B L I C
;;;============================================================================

(defn str->person
  "Expects a string containing a delimited list of fields in the order
  LastName FirstName Gender FavoriteColor DateOfBirth
  where valid delims are pipe, comma, or space and date format is MM/dd/yyyy.
  Returns a map that conforms to the :sorted.person/person spec or a Failure
  object."
  [s]
  (f/if-let-ok? [person (f/ok-> s
                                split-on-delims
                                strs->vals
                                vals->person)]
                person
                (f/fail "Error in str->person: Could not parse `%s`\n%s"
                        s (f/message person))))

(defn person->str
  "Expects a map that conforms to :sorted.person/person.
  Returns a string representation of those values delimited by delim."
  [{::keys [last-name first-name gender fav-color dob] :as person} delim]
  (let [first4 (join delim [last-name first-name gender fav-color])]
    (f/if-let-ok? [dob-str (jt/format formatter dob)]
                  (join delim [first4 dob-str])
                  (f/fail "Error in person->str: Could not convert `%s`\n%s"
                          person
                          (f/message dob-str)))))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(s/def ::delim-str (s/with-gen string? #(s/gen #{"|" "," " "})))
(s/def ::delim-regex (s/with-gen
                       #(instance? java.util.regex.Pattern %)
                       #(s/gen #{#"|" #"," #" "})))

#_(def min-day (.. java.time.LocalDate MIN toEpochDay))
#_(def max-day (.. java.time.LocalDate MAX toEpochDay))
(def min-day  -719162)  ; 01/01/0001
(def max-day  2932896)  ; 12/31/9999
(def date-gen (gen/fmap #(. java.time.LocalDate ofEpochDay %)
                        (gen/choose min-day max-day)))

(s/def ::date (s/with-gen #(instance? java.time.LocalDate %) (constantly date-gen)))

(s/def ::date-str
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

;; Some regex matchers that will determine if a string appears to be a
;; well-formed delimited string that could represent a ::person
(def non-ws-delims #".*?([\|,]).*?\1.*?\1.*?\1\s*\d{1,2}/\d{1,2}/[\+\-]*\d{1,9}\s*$")
(def ws-delims     #".*(\s+).*\s+.*\s+.*\s+\d{1,2}/\d{1,2}/[\+\-]*\d{1,9}\s*$")

(s/def ::person-str
  (s/with-gen (s/and string?
                     #(or (re-matches non-ws-delims %)
                          (re-matches ws-delims %)))
    #(gen/fmap (fn [[last first gender color dob delim]]
                 (join delim [last first gender color (jt/format formatter dob)]))
               (gen/tuple non-delim                               ; Last
                          non-delim                               ; First
                          non-delim                               ; Gender
                          non-delim                               ; Color
                          date-gen                                ; DoB
                          (gen/elements ["|" "," " "])))))        ; Delim

(s/fdef str->person
  :args (s/cat :s ::person-str)
  :ret  (s/or :success ::person
              :failure f/failed?))

(s/fdef person->str
  :args (s/cat :p ::person :d ::delim-str)
  :ret  (s/or :success ::person-str
              :failure f/failed?)
  ;; Verify that we can turn the ret val back into the original ::person
  :fn #(let [return (-> % :ret second)]
         (or (f/failed? return)
             (= (str->person return) (-> % :args :p)))))

(s/fdef epoch->str
  :args (s/cat :d (s/and int? #(>= % min-day) #(<= % max-day)))
  :ret (s/or :success ::date-str
             :failure f/failed?))

(s/fdef no-delims?
  :args (s/cat :s string?)
  :ret boolean?
  :fn (let [ds [#" " #"," #"\|"]
            found-delim? (fn [result delim] (re-find delim (-> result :args :s)))]
        (s/or :true (s/and #(:ret %)
                           ;; No delims found in the original string.
                           (fn [result] (not-any? #(found-delim? result %) ds)))
              :false (s/and #(not (:ret %))
                            ;; At least one delim found in original string.
                            (fn [result] (some #(found-delim? result %) ds))))))

(s/fdef split-trim
  :args (s/cat :s ::person-str :delim ::delim-regex)
  :ret (s/or :success (s/coll-of string?)
             :failure f/failed?))

(comment
  (expound/explain-results (st/check 'sorted.person/person->str))

  (expound/explain-results (st/check 'sorted.person/no-delims?
                                     {:clojure.spec.test.check/opts
                                      {:num-tests 1000}}))
  )
