(ns potoo.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [bidi.ring :refer [make-handler]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :as resp]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth.backends.session :refer [session-backend]]
            [potoo.datomic :as db]
            [taoensso.timbre :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

;; Helpers

(defn- fmt-potoos [data]
  (map (partial zipmap [:text :name :date]) data))

(defn unauthorized []
  {:status  401
   :headers {}
   :body    nil})

;; Handlers

(defn get-potoos [req]
  (let [data (db/find-potoos (:db-conn req))]
    (log/info "Getting all potoos from" (:remote-addr req))
    (resp/response (fmt-potoos data))))

(defn create-potoo [req]
  (let [name (:identity req)
        date (c/to-date (t/now))
        text (-> req :body :text)
        _ (db/create-potoo (:db-conn req) text name date)]
    (resp/response {:text text :name name :date date})))

(defn create-session [req]
  (let [conn (:db-conn req)
        {:keys [username password]} (-> req :body)
        session (assoc (:session req) :identity username)]
    (if (db/find-user-id-by-name-and-pass conn username password)
      (-> (resp/response {:name username})
          (assoc :session session))
      (unauthorized))))

(defn index-handler [_]
  (resp/file-response "index.html" {:root "resources/public"}))

;; Routes

(def routes
  ["/" {""     index-handler
        "api/" {"potoos" {:get get-potoos
                          :post create-potoo}
                "sessions" {:post create-session}}}])

;; Primary handler

(def backend (session-backend))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :db-conn conn))))

(defn potoo-handler [conn]
  (-> (make-handler routes)
      (wrap-connection conn)
      (wrap-resource "public")
      (wrap-json-body {:keywords? true :bigdecimals? true})
      (wrap-authentication backend)
      wrap-session
      wrap-json-response))

;; WebServer

(defrecord WebServer [opts container datomic-connection]
  component/Lifecycle
  (start [component]
    (log/info "Starting web server with params:" opts)
    (let [conn (:db-conn datomic-connection)]
      (let [req-handler (potoo-handler conn)
            container (run-jetty req-handler opts)]
        (assoc component :container container))))
  (stop [component]
    (log/info "Stopping web server")
    (.stop container)
    component))

(defn new-server [opts]
  (WebServer. opts nil nil))
