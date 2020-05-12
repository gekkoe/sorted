(ns sorted.server-test
  (:require [clojure.test :refer :all]
            [failjure.core :as f]
            [sorted.helpers :as h]
            [sorted.server :as svr]))

;; NOTE: The double with-defs clauses here are to prevent the reset! operation in
;;       start-server! from seeing the wrong server, which causes tests to step
;;       on the real active server.

(deftest get-port-test
  (let [port (h/get-free-port)]
    (with-redefs [svr/server (atom nil)]
      (with-redefs [svr/server (atom (svr/start-server! port))]
        (testing "Returns the correct port"
          (is (= port (svr/get-port))))

        ;; Be sure to stop test server before leaving with-redefs
        (svr/stop-server!)))))

(deftest start-server!-test
  (let [port (h/get-free-port)]
    (with-redefs [svr/server (atom nil)]
      (testing (str "Returns an org.eclipse.jetty.server.Server object on "
                    "success. Returns a Failure object if something fails.")
        (let [server (svr/start-server! port)]
          (is (instance? org.eclipse.jetty.server.Server server))
          (svr/stop-server!)))
      (testing "Returns a Failure object on failure"
        (let [impossible-port 90000]
          (is (f/failed? (svr/start-server! impossible-port)))))

      ;; Be sure to stop test server before leaving with-redefs
      (svr/stop-server!))))

(deftest stop-server!-test
  (with-redefs [svr/server (atom nil)]
    (with-redefs [svr/server (atom (svr/start-server! (h/get-free-port)))]
      (let [result (svr/stop-server!)]
        (testing "Sets svr/server to nil"
          (is (nil? @svr/server)))
        (testing "Returns nil"
          (is (nil? result)))))))
