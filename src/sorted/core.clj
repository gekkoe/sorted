(ns sorted.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :refer [join]]
            [sorted.person :as p]
            [sorted.fileio :as file]
            [failjure.core :as f])
  (:gen-class))

(def usage
  (str "Usage: sorted [options] file1 file2 file3\n"
       "This program can support more than 3 files if desired, "
       "but at least one must be provided.\n\n"))

(def cli-options
  ;; An option with a required argument
  [#_["-p" "--port PORT" "Port number. If not specified, doesn't start server."
      :parse-fn #(Integer/parseInt %)
      :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-d" "--dob" "Sort output by date of birth, ascending."]
   ["-g" "--gender" "Sort by gender, female first, then by last name ascending."]
   ["-l" "--last" "Sort output by last name, descending."]
   ["-h" "--help" "Prints this help text.\n\n"]])

(defn -main
  "Expects to be passed in files in one of three formats found in the fileio
  namespace. Parses these files and prints out the people in them sorted one of
  three ways depending on which command line option is selected."
  [& args]
  (let [opts (parse-opts args cli-options)
        file-names (:arguments opts)]
    (cond
      (or (get-in opts [:options :help])
          (empty? file-names)) (println (str usage (:summary opts)))
      (:errors opts) (println (join "\n" (:errors opts)))
      :else (->> (map file/text-read file-names)
                 (map (partial map p/str->person))
                 flatten
                 (remove f/failed?)))))

(comment
  (map #(p/person->str % " ") (sort-by ::p/dob #(compare %2 %1)
                                       (-main "env/dev/resources/space-delim")))
  )
