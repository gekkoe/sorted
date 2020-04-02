(defproject sorted "0.1.0-SNAPSHOT"
  :description "sorted - A simple program to sort people."
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :main ^:skip-aot sorted.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
