(defproject sorted "0.1.0-SNAPSHOT"
  :description "sorted - A simple program to sort people."

  :dependencies [[cheshire "5.10.0"]
                 [clj-time "0.15.2"]
                 [compojure "1.6.1"]
                 [http-kit "2.3.0"]
                 [liberator "0.15.3"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/spec.alpha "0.2.187"]
                 [org.clojure/test.check "1.0.0"]
                 [ring/ring-core "1.8.0"]
                 [ring/ring-defaults "0.3.2"]]

  :main ^:skip-aot sorted.core

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
