(ns sorted.server
  (:require [failjure.core :as f]
            [ring.adapter.jetty :refer [run-jetty]]
            [sorted.handler :refer [handler]]
            [clojure.spec.alpha :as s]))

(def server (atom nil))
(def default-port 3000)

(defn get-port
  "Returns the port of the running server. Returns nil if no server is running."
  []
  (when @server
    (let [cs (. ^org.eclipse.jetty.server.Server @server getConnectors)
          c (first cs)
          port (. ^org.eclipse.jetty.server.ServerConnector c getLocalPort)]
      port)))

(defn start-server!
  "Attempts to start the web server on port. If no port is passed, defaults to
  sorted.server/default-port. Returns an org.eclipse.jetty.server.Server object
  on success.
  Returns a Failure object if unsuccessful."
  ([] (start-server! default-port))
  ([port]
   (f/if-let-ok? [result
                    (f/try*
                     (if @server
                       @server
                       (let [svr (run-jetty handler {:port port :join? false})]
                         (reset! server svr))))]
                   result
                   (f/fail (format "Unable to open server on port %s\n%s"
                                   port
                                   (f/message result))))))

(defn stop-server!
  "If the web server is running, will stop it.
  Returns nil on success.
  Returns a Failure object if something goes wrong."
  []
  (f/try*
   (when @server
     (do
       (.stop ^org.eclipse.jetty.server.Server @server)
       (reset! server nil)))))

(defn restart-server!
  "Stops & restarts the web server if one is running. Otherwise starts a new one.
  Returns a Failure object if something goes wrong."
  ([] (restart-server! default-port))
  ([port]
   (stop-server!)
   (start-server! port)))
