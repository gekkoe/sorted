(defproject sorted "2.0.0"
  :description "sorted - A simple program to sort people."

  :dependencies [[ch.qos.logback/logback-classic "1.2.3"]
                 [clojure.java-time "0.3.2"]
                 [compojure "1.6.1"]
                 [expound "0.8.4"]
                 [failjure "2.0.0"]
                 [hiccup "1.0.5"]
                 [liberator "0.15.3"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/test.check "1.0.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.clojure/tools.logging "1.1.0"]
                 [prone "2020-01-17"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-jetty-adapter "1.8.1"]]

  :target-path "target/%s/"
  :main ^:skip-aot sorted.core

  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler sorted.handler/handler}

  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "sorted.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev   {:jvm-opts []
                   :dependencies [[java-time-literals "2018-04-06"]
                                  [ring/ring-devel "1.8.0"]
                                  [ring/ring-mock "0.4.0"]]

                   :plugins [[jonase/eastwood "0.3.5"]]
                   :source-paths ["env/dev/clj"]
                   :resource-paths ["env/dev/resources"]
                   :repl-options {:init-ns user
                                  :timeout 120000}
                   :injections [(.. System (setProperty "prone.enabled" "true"))]}

   :project/test  {:jvm-opts []
                   :resource-paths ["env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
