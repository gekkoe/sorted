(ns sorted.core
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clojure.string :refer [join]]
            [failjure.core :as f]
            [sorted.person :as p]
            [sorted.fileio :as file]
            [sorted.people :as ppl]
            [sorted.server :as svr])
  (:gen-class))

;;;============================================================================
;;;                           H E L P E R S
;;;============================================================================

(def cli-options
  [["-d" "--dob" "Sort output by date of birth, ascending."]
   ["-g" "--gender" "Sort by gender, female first, then by last name ascending."]
   ["-h" "--help" "Prints this help text..."]
   ["-l" "--last" "Sort output by last name, descending."]
   ["-p" "--port PORT" "Port number. If not specified, doesn't start server."
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1023 % 49152) "Must be a number between 1024 and 49151"]]])

(defn usage [options-summary]
  (->> ["sorted - A simple program for sorting people."
        ""
        "Usage: sorted [options] file1 file2 file3 ..."
        ""
        "Options:"
        options-summary
        ""
        (str "This program can support more than 3 files if desired, "
             "but at least one must be provided.\n")]
       (join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (join \newline errors)))

(defn validate-args
  "Validate command line. Either return a map indicating the program should
  exit (with a error message, and optional ok status), or a map of instructions."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        {:keys [help dob gender last port]} options
        sort-count (->> [dob gender last] (filter true?) count)]
    (cond
      help                                               ; Help
      {::exit-msg (usage summary) ::ok? true}
      errors                                             ; Errors
      {::exit-msg (str (error-msg errors) "\n\n" (usage summary))}
      (> sort-count 1)                                   ; Too many sort options
      {::exit-msg "Please select no more than one sort option."}
      (and (empty? options) (empty? arguments))          ; No commands
      {::exit-msg (usage summary)}
      (empty? arguments)
      {::exit-msg "Please provide at least one file with person records in it."}
      port                                               ; Start server
      {::port port ::files arguments}
      :else {::sort-kw (cond dob    ::p/dob              ; Sort file(s)
                             gender ::p/gender
                             last   ::p/last-name
                             :else  nil)
             ::files arguments})))

(defn system-exit [status] (System/exit status))

(defn exit [status msg]
  (println msg)
  (when-not (zero? status)
    (log/warn msg))
  (system-exit status))

(defn sorted-people-str
  "Passed a :sorted.people/sort-kw, returns a string conataining a sorted
  representation of the people list."
  [sort-kw]
  (->> (ppl/sorted-by sort-kw)
       (map #(p/person->str % " "))
       (join \newline)))

;;;============================================================================
;;;                               M A I N
;;;============================================================================

(defn -main
  "Expects to be passed in one or more files delimited by a string conforming to
  sorted.person/delim-str. Parses these files into person records, ignoring any
  lines it cannot parse. If a port is passed in, starts a server to display and
  allow input of new person records. Otherwise prints out the people in the
  file(s), possibly sorted, depending on command line options."
  [& args]
  (let [{::keys [sort-kw files port exit-msg ok?]} (validate-args args)]
    (if (not exit-msg)
      (do
        (ppl/load-from-files! files)
        (cond
          (zero? (count @ppl/people))             ; No files
          (exit 1 (format "Unable to parse any people from the file%s provided."
                          (if (> (count files) 1) "s" "")))
          port                                    ; Open server
          (do
            (printf "Server started on port %d.\nLogging to log/sorted.log.\n"
                    port)
            (flush)
            (svr/start-server! port))
          :else                                   ; Just display sorted files.
          (exit 0 (sorted-people-str sort-kw))))
      (exit (if ok? 0 1) exit-msg))))             ; Exit with possible error.

;;;============================================================================
;;;                        S P E C    R E L A T E D
;;;============================================================================

(s/def ::args (s/coll-of string?))
(s/def ::exit-msg string?)
(s/def ::ok? boolean?)
(s/def ::exit-instructions (s/keys :req [::exit-msg] :opt [::ok?]))
(s/def ::files (s/coll-of string?))
(s/def ::port (s/and int? #(< 0 % 0x10000)))
(s/def ::actions (s/keys :req [::files] :opt [::ppl/sort-kw ::port]))

(s/fdef usage
  :args (s/cat :summary string?)
  :ret string?
  :fn #(let [summary (-> % :args :summary)
             ret (-> % :ret)]
         (and (not= summary ret)
              (.contains ^String ret summary))))

(s/fdef error-msg
  :args (s/cat :errors (s/coll-of string? :into []))
  :ret string?
  :fn #(let [errors (join \newline (-> % :args :errors))
             ret (-> % :ret)]
         (and (not= errors ret)
              (.contains ^String ret errors))))

(s/fdef validate-args
  :args (s/cat :args ::args)
  :ret (s/or :exit   ::exit-instructions
             :action ::actions))

(s/fdef sorted-people-str
  :args ::ppl/sort-kw
  :ret string?)
