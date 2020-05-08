(ns sorted.server
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [sorted.handler :refer [handler]]))

(def server (atom nil))
(def default-port 3000)

(defn start-server!
  ([] (start-server! default-port))
  ([port]
   (if @server
     (.start ^org.eclipse.jetty.server.Server @server)
     (let [svr (run-jetty handler {:port port :join? false})]
       (reset! server svr)))))

(defn stop-server! [] (if @server
                        (do
                          (.stop ^org.eclipse.jetty.server.Server @server)
                          (reset! server nil))))

(defn restart-server!
  ([] (restart-server! default-port))
  ([port]
   (stop-server!)
   (start-server! port)))
