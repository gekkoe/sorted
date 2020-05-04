(defproject sorted "1.0.0-SNAPSHOT"
  :description "sorted - A simple program to sort people."

  :dependencies [[cheshire "5.10.0"]
                 [clojure.java-time "0.3.2"]
                 [compojure "1.6.1"]
                 [cprop "0.1.16"]
                 [expound "0.8.4"]
                 [http-kit "2.3.0"]
                 ;[liberator "0.15.3"]
                 [mount "0.1.16"]
                 [nrepl "0.7.0"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/spec.alpha "0.2.187"]
                 [org.clojure/test.check "1.0.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 ;[ring/ring-core "1.8.0"]
                 ;[ring/ring-defaults "0.3.2"]
                 [failjure "2.0.0"]]

  :target-path "target/%s/"
  :main ^:skip-aot sorted.core

  :plugins []

  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "sorted.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev   {:jvm-opts ["-Dconf=dev-config.edn"]
                   :dependencies [[org.clojure/java.classpath "1.0.0"]]
                   :plugins []
                   :source-paths ["env/dev/clj"]
                   :resource-paths ["env/dev/resources"]
                   :repl-options {:init-ns user
                                  :timeout 120000}
                   :injections []}

   :project/test  {:jvm-opts ["-Dconf=test-config.edn"]
                   :resource-paths ["env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
