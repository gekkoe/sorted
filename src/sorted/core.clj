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

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number. If not specified, doesn't start server."
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1023 % 49152) "Must be a number between 1024 and 49151"]]
   ["-d" "--dob" "Sort output by date of birth, ascending."]
   ["-g" "--gender" "Sort by gender, female first, then by last name ascending."]
   ["-l" "--last" "Sort output by last name, descending."]
   ["-h" "--help" "Prints this help text..."]])

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
        sort-count (->> [dob gender last]
                        (filter true?)
                        count)]
    (cond
      help ; help => exit OK with usage summary
      {::exit-msg (usage summary) ::ok? true}
      errors ; errors => exit with description of errors
      {::exit-msg (str (error-msg errors) "\n\n" (usage summary))}
      ;; Make sure no more than one sort option is indicated
      (> sort-count 1)
      {::exit-msg "Please select no more than one sort option."}
      (and (empty? options) (empty? arguments))
      {::exit-msg (usage summary)}
      (empty? arguments)
      {::exit-msg "Please provide at least one file with person records in it."}
      port ; port => start the server on indicated port
      {::port port ::files arguments}
      :else {::sort-kw (cond dob    ::p/dob
                             gender ::p/gender
                             last   ::p/last-name
                             :else  nil)
             ::files arguments})))

(defn system-exit [status]
  (System/exit status))

(defn exit [status msg]
  (println msg)
  (when-not (zero? status)
    (log/error msg))
  (system-exit status))

(defn sorted-people-str
  "Passed a :sorted.people/sort-kw, sorts the people list by the kw, then returns
  a string conataining the sorted list."
  [sort-kw]
  (->> (ppl/sorted-by sort-kw)
       (map #(p/person->str % " "))
       (join \newline)))

(defn -main
  "Expects to be passed in one or more files delimited by a string conforming to
  sorted.person/delim-str. Parses these files into person records in
  sorted.people/people, ignoring any lines it cannot parse. If a port is passed
  in, starts a server to display and allow input of new person records.
  Otherwise prints out the people in the file(s), possibly sorted one of three
  ways depending on which command line option is selected."
  [& args]
  (let [{::keys [sort-kw files port exit-msg ok?]} (validate-args args)]
    (if exit-msg
      (exit (if ok? 0 1) exit-msg)
      (if (and (ppl/load-from-files! files) (pos? (count @ppl/people)))
        (if port
          (do (printf "Server started on port %d.\nLogging to log/sorted.log.\n"
                      port)
              (svr/start-server! port))
          (exit 0 (sorted-people-str sort-kw)))
        (exit 1 (format "Unable to parse any people from the file%s provided."
                        (if (> (count files) 1) "s" "")))))))

;;;============================================================================
;;;                              S P E C S
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
