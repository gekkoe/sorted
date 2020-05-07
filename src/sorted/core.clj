(ns sorted.core
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :refer [join]]
            [ring.adapter.jetty :refer [run-jetty]]
            [sorted.person :as p]
            [sorted.fileio :as file]
            [sorted.handler :refer [handler]]
            [sorted.people :refer [people sorted-by]]
            [failjure.core :as f]
            [sorted.people :as ppl])
  (:gen-class))

(def server (atom nil))

(defn start-server!
  ([] (start-server! 3000))
  ([port]
   (if @server
     (.start @server)
     (let [svr (run-jetty handler {:port port :join? false})]
       (swap! server (constantly svr))))))

(defn stop-server! [] (if @server
                       (do (.stop @server)
                           (swap! server (constantly nil)))))

(defn restart-server!
  ([] (restart-server! 3000))
  ([port]
   (stop-server!)
   (start-server! port)))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number. If not specified, doesn't start server."
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
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
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the order to sort output and the files for input."
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
      port ; port => start the server on indicated port
      {::port port ::files arguments}
      (empty? arguments)
      {::exit-msg "Please provide at least one file with people records in it."}
      :else {::ppl/sort-kw (cond dob    ::p/dob
                                 gender ::p/gender
                                 last   ::p/last-name
                                 :else  nil)
             ::files arguments})))

(defn system-exit [status]
  (System/exit status))

(defn exit [status msg]
  (println msg)
  (system-exit status))

(defn load-files!
  [files]
  (swap! people (fn [_] (vec
                         (->> (map file/text-read files)
                              (map (partial map p/str->person))
                              flatten
                              (remove f/failed?))))))

(defn -main
  "Expects to be passed in one or more files delimited by a string conforming to
  sorted.person/delim-str. Parses these files into person records in the atom
  people, ignoring any lines it cannot parse. If a port is passed in, starts a
  server to display and allow input of new person records. Otherwise prints out
  the people in the file(s), possibly sorted one of three ways depending on
  which command line option is selected."
  [& args]
  (let [{::keys [sort-kw files port exit-msg ok?]} (validate-args args)]
    (cond
      exit-msg (exit (if ok? 0 1) exit-msg)
      port     (if (and (load-files! files) (pos? (count @people)))
                 (start-server! port)
                 (exit 1 (format "Unable to parse any people from the file%s provided."
                                 (if (> (count files) 1) "s" ""))))
      ;; Assuming we're able to load at least 1 person, display them.
      files    (if (and (load-files! files) (pos? (count @people)))
                 (->> (sorted-by sort-kw)
                      (map #(p/person->str % " "))
                      (join \newline)
                      (exit 0))
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
(s/def ::action-instructions (s/keys :req [::ppl/sort-kw ::files]))

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
