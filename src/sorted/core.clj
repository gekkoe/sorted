(ns sorted.core
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :refer [join]]
            [sorted.person :as p]
            [sorted.fileio :as file]
            [failjure.core :as f])
  (:gen-class))

(def people (atom []))

(def cli-options
  ;; An option with a required argument
  [#_["-p" "--port PORT" "Port number. If not specified, doesn't start server."
      :parse-fn #(Integer/parseInt %)
      :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-d" "--dob" "Sort output by date of birth, ascending."]
   ["-g" "--gender" "Sort by gender, female first, then by last name ascending."]
   ["-l" "--last" "Sort output by last name, descending."]
   ["-h" "--help" "Prints this help text.\n\n"]])

(defn usage [options-summary]
  (->> ["sorted - A simple program for sorting people."
        ""
        "Usage: sorted [options] file1 file2 file3 ..."
        ""
        "Options:"
        options-summary
        ""
        "This program can support more than 3 files if desired, "
        "but at least one must be provided.\n\n"]
       (join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the order to sort output and the files for input."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        {:keys [help dob gender last]} options
        sort-count (->> [dob gender last]
                        (filter true?)
                        count)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {::exit-message (usage summary) ::ok? true}
      errors ; errors => exit with description of errors
      {::exit-message (error-msg errors)}
      ;; Make sure no more than one sort option in indicated
      (> sort-count 1)
      {::exit-message "Please select no more than one sort option."}
      (empty? arguments)
      {::exit-message "Please provide at least one file with people records in it."}
      :else {::sort-kw (cond dob    ::p/dob
                             gender ::p/gender
                             last   ::p/last-name
                             :else  nil)
             ::files arguments})))

(defn exit [status msg]
  (println msg)
  #_(System/exit status))

(defn load-files!
  [files]
  (swap! people (fn [_] (vec
                         (->> (map file/text-read files)
                              (map (partial map p/str->person))
                              flatten
                              (remove f/failed?))))))

(defn sort-people
  [sort-kw]
  (if-let [order (case sort-kw
                   ::p/dob compare
                   ::p/gender compare
                   ::p/last-name #(compare %2 %1)
                   nil)]
    (vec (sort-by sort-kw order @people))
    @people))

(defn -main
  "Expects to be passed in files in one of three formats found in the fileio
  namespace. Parses these files and prints out the people in them sorted one of
  three ways depending on which command line option is selected."
  [& args]
  (let [{::keys [sort-kw files exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (when (load-files! files)
        (->> (sort-people sort-kw)
             (map #(p/person->str % " "))
             (join \newline)
              println)))))

;;;============================================================================
;;;                              S P E C S
;;;============================================================================

(s/def ::args (s/coll-of string?))
(s/def ::exit-message string?)
(s/def ::ok? boolean?)
(s/def ::exit-instructions (s/keys :req [::exit-message] :opt [::ok?]))
(s/def ::sort-kw (s/nilable keyword?))
(s/def ::files (s/coll-of string?))
(s/def ::action-instructions (s/keys :req [::sort-kw ::files]))

(s/fdef usage
  :args (s/cat :summary string?)
  :ret string?
  :fn #(let [summary (-> % :args :summary)
             ret (-> % :ret)]
         (and (not= summary ret)
              (.contains ret summary))))

(s/fdef error-msg
  :args (s/cat :errors coll?)
  :ret string?
  :fn #(let [errors (join \newline (-> % :args :errors))
             ret (-> % :ret)]
         (and (not= errors ret)
              (.contains ret errors))))

(s/fdef validate-args
  :args (s/cat :args ::args)
  :ret (s/or :exit   ::exit-instructions
             :action ::action-instructions))

(s/fdef sort-people
  :args (s/cat :sort-kw ::sort-kw)
  :ret (s/coll-of string?))
