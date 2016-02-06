(ns potoo.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [bidi.ring :refer [make-handler]]
            [potoo.datomic :as db]))

;; Handlers

(defn get-potoos [req]
  (db/find-potoos (:db-conn req)))

;; Routes

(def routes
  ["/api" {"potoos" {:get {[""] get-potoos}}}])

;; Primary handler

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :db-conn conn))))

(defn potoo-handler [conn]
  (wrap-connection (make-handler routes) conn))

;; WebServer

(defrecord WebServer [port container datomic-connection]
  component/Lifecycle
  (start [component]
    (let [conn (:db-conn datomic-connection)]
      (let [req-handler (potoo-handler conn)
            container (run-jetty req-handler
                                 {:port port :join? false})]
        (assoc component :container container))))
  (stop [component]
    (.stop container)
    component))

(defn new-server [web-port]
  (WebServer. web-port nil nil))