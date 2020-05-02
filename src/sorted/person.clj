(ns sorted.person
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :refer [split trim join]]
            [failjure.core :as f]
            [java-time :as jt]
            [clojure.spec.test.alpha :as st]))

;;;============================================================================
;;;                          H E L P E R S
;;;============================================================================

;; NOTE: If these change, it'll be necessary to update doc strings throughout
;; this file.
(def line-breaks #"[\n\r]")
(def formatter (jt/formatter "MM/dd/yyyy"))
(def delim-map {"|" #"\|"
                 "," #","
                 " " #" "})
(def delim-str-set (into #{} (keys delim-map)))
(def delim-regex (re-pattern (str "[" (join (keys delim-map)) "]")))
(def delim-regex-set (into #{} (vals delim-map)))
(def delim-regex-str-set (into #{} (map str delim-regex-set)))

;; Some regex matchers that will determine if a string appears to be a
;; well-formed delimited string that could represent a ::person
(def non-ws-delims
  #".*?([|,]).*?\1.*?\1.*?\1\s*\d{1,2}/\d{1,2}/[\+\-]*\d{1,9}\s*$")
(def ws-delims
  #".*(\s+).*\s+.*\s+.*\s+\d{1,2}/\d{1,2}/[\+\-]*\d{1,9}\s*$")

(defn no-delims?
  "Searches a string for any instance of the delimiters pipe, comma, or space.
  Returns true if none are found, or if an invalid value is passed as s. Returns
  false if one or more is found."
  [s]
  (if (string? s)
    (not (re-find delim-regex s))
    true))

(defn split-trim
  "Splits a string using a delimiter and trims any extra whitespace from the
  front and back of each substring. Returns a vector of the substrings.
  Returns a Failure object if unsuccessful."
  [s delim]
  (if (and (s/valid? ::person-str s)
           (s/valid? ::delim-regex delim)
           (re-find delim s))
    (mapv trim (split s delim))
    (f/fail "Error in split-trim: Could not split person `%s` with delim `%s`"
            s delim)))

(defn split-on-delims
  "Attempts to split the string s into a vector of strings. If delim is passed,
  will attempt to use it as the delim for splitting. If delim is either not
  passed, or is invalid, attempts to use pipe, comma, or space as delims, in
  that order.
  Returns a Failure object if unsuccessful."
  ([s] (split-on-delims s nil))
  ([s delim]
   (f/if-let-ok? [result (f/try*
                          (if-let [d-regex (delim-map delim)]
                            (split-trim s d-regex)
                            (cond (re-find #"\|" s) (split-trim s #"\|")
                                  (re-find #"," s)  (split-trim s #",")
                                  :else             (split-trim s #" "))))]
                 (vec result)
                 (f/fail "Error in split-on-delims: Could not parse `%s`\n%s"
                         s (f/message result)))))

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
  conforming to sorted.person/person.
  Returns a Failure object if unsuccessful."
  [vs]
  (if (s/valid? ::person-vals vs)
    (zipmap [::last-name ::first-name ::gender ::fav-color ::dob] vs)
    (f/fail (str "Error in vals->person: `%s` is not a valid "
                 ":sorted.person/person-vals.") vs)))

;;;============================================================================
;;;                           P U B L I C
;;;============================================================================

(defn str->person
  "Expects a string containing a delimited list of fields in the order
  LastName FirstName Gender FavoriteColor DateOfBirth
  where valid delims are pipe, comma, or space and date format is MM/dd/yyyy.
  If delim is provided, will attempt to parse using that delim, but if it
  fails will attemp to use pipe, comma, or space, in that order.
  Returns a map that conforms to the :sorted.person/person spec or a Failure
  object."
  ([s]
   (str->person s nil))
  ([s delim]
   (f/if-let-ok? [person (f/ok-> s
                                 (split-on-delims delim)
                                 strs->vals
                                 vals->person)]
                 person
                 (f/fail "Error in str->person: Could not parse `%s`\n%s"
                         s (f/message person)))))

(defn person->str
  "Expects a map that conforms to :sorted.person/person.
  Returns a string representation of those values delimited by delim."
  [{::keys [last-name first-name gender fav-color dob] :as person} delim]
  (if (s/valid? ::person person)
    (let [first4 (join delim [last-name first-name gender fav-color])]
      (f/if-let-ok? [dob-str (f/try* (jt/format formatter dob))]
                    (join delim [first4 dob-str])
                    (f/fail "Error in person->str: Could not convert `%s`\n%s"
                            person
                            (f/message dob-str))))
    (f/fail (str "Error in person->str: `%s` is not a valid "
                 ":sorted.person/person.") person)))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(s/def ::delim-str
  (s/with-gen
    (s/and string? delim-str-set)
    #(gen/one-of [(s/gen string?) (s/gen delim-str-set)])))

;; java.util.regex.Pattern objects don't eval as = unless they are the same
;; object. so we convert them to strings for comparison.
(s/def ::delim-regex (s/with-gen
                       (s/and
                        #(instance? java.util.regex.Pattern %)
                        #(delim-regex-str-set (str %)))
                       #(gen/one-of [(s/gen delim-regex-set)
                                     (s/gen #{#"bad" #"vals" #"here"})])))

#_(def min-day (.. java.time.LocalDate MIN toEpochDay))
#_(def max-day (.. java.time.LocalDate MAX toEpochDay))
(def min-day  -719162)  ; 01/01/0001
(def max-day  2932896)  ; 12/31/9999
(def epoch-day-gen (gen/choose min-day max-day))
(def date-gen (gen/fmap #(. java.time.LocalDate ofEpochDay %) epoch-day-gen))
(def date-str-gen (gen/fmap #(jt/format formatter %) date-gen))

(s/def ::date (s/with-gen #(instance? java.time.LocalDate %)
                          (constantly date-gen)))

(s/def ::date-str
  (s/with-gen (s/and string? #(f/ok? (f/try* (jt/local-date formatter %))))
              (constantly date-str-gen)))

(s/def ::no-delim-str (s/and string? no-delims?))
(s/def ::last-name ::no-delim-str)
(s/def ::first-name ::no-delim-str)
(s/def ::gender ::no-delim-str)
(s/def ::fav-color ::no-delim-str)
(s/def ::dob ::date)
(s/def ::person
  (s/keys :req [::first-name ::last-name ::gender ::fav-color ::dob]))
(s/def ::person-strs
  (s/tuple ::first-name ::last-name ::gender ::fav-color ::date-str))
(s/def ::person-vals
  (s/tuple ::first-name ::last-name ::gender ::fav-color ::dob))

;; NOTE: This generator is rather specific, so it has a lowered likelihood of
;; working. It requires an override to max-tries. Without it, test.check will
;; only attempt it 10 times, and will generally fail on string or string-ascii.
;; It does work for string-alphanumeric though.
;; Also worth noting, line breaks throw off the regex matchers, but it seems
;; reasonable, since this is line-by-line input, to exclude this case from
;; the generative tests.
(def non-delim-gen (gen/such-that #(and (no-delims? %)
                                        (not (re-find line-breaks %)))
                                  (gen/string) 100))

(s/def ::person-str
  (s/with-gen (s/and string?
                     #(not (re-find line-breaks %))
                     #(or (re-matches non-ws-delims %)
                          (re-matches ws-delims %)))
              #(gen/fmap
                (fn [[last first gender color dob delim]]
                  (join delim [last
                               first
                               gender
                               color
                               (jt/format formatter dob)]))
                (gen/tuple non-delim-gen                           ; Last
                           non-delim-gen                           ; first
                           non-delim-gen                           ; gender
                           non-delim-gen                           ; color
                           date-gen                                ; dob
                           (gen/elements (vec delim-str-set))))))  ; delim

(s/fdef str->person
  :args (s/or :unary (s/cat :s any?)
              :binary (s/cat :s ::person-str :delim ::delim-str))
  :ret  (s/or :success ::person
              :failure f/failed?)
  ;; Verify that the person generatated has vals that correspond to those parsed
  ;; from the original string.
  :fn #(let [ret (-> % :ret second)
             s (-> % :args second :s)
             delim (-> % :args second :delim)
             values (split-on-delims s delim)]
         (or (f/failed? ret)
             (= values (f/ok-> ret
                               (person->str (rand-nth (vec delim-str-set)))
                               split-on-delims)))))

(s/fdef person->str
  :args (s/cat :p ::person :d ::delim-str)
  :ret  (s/or :success ::person-str
              :failure f/failed?)
  ;; Verify that we can turn the ret val back into the original ::person
  :fn #(let [ret (-> % :ret second)
             p (-> % :args :p)]
         (or (f/failed? ret)
             (= (str->person ret) p))))

;; This spec may be a bit redundant/silly. I mainly wrote it to learn about
;; writing specs for predicates.
(s/fdef no-delims?
  :args (s/cat :s any?)
  :ret boolean?
  :fn (let [ds delim-regex-set
            found? (fn [result delim]
                     (f/if-let-ok?
                      [f (f/try* (re-find delim (-> result :args :s)))]
                      f
                      false))]
        (s/or :true  (s/and #(:ret %)
                            ;; No delims found in s.
                            (fn [result] (not-any? #(found? result %) ds)))
              :false (s/and #(not (:ret %))
                            ;; At least one delim found in s.
                            (fn [result] (some #(found? result %) ds))))))

(s/fdef split-trim
  :args (s/cat :s ::person-str :delim ::delim-regex)
  :ret (s/or :success ::person-strs
             :failure f/failed?))

(s/fdef split-on-delims
  :args (s/or :unary  (s/cat :s ::person-str)
              :binary (s/cat :s ::person-str
                             :delim ::delim-str))
  :ret (s/or :success ::person-strs
             :failure f/failed?))

(s/fdef strs->vals
  :args (s/cat :ss ::person-strs)
  :ret (s/or :success ::person-vals
             :failure f/failed?))

(s/fdef vals->person
  :args (s/cat :vs ::person-vals)
  :ret (s/or :success ::person
             :failure f/failed?))

(comment
  (expound/explain-results (st/check 'sorted.person/person->str))

  (expound/explain-results (st/check 'sorted.person/no-delims?
                                     {:clojure.spec.test.check/opts
                                      {:num-tests 1000}}))
  )
