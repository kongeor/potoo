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
            [buddy.hashers :as hashers]
            [potoo.db :as db]
            [taoensso.timbre :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

;; Helpers

(defn unauthorized []
  {:status  401
   :headers {}
   :body    nil})

(defn- encrypt [password]
  (hashers/derive password {:alg :bcrypt+blake2b-512}))

;; Handlers

(defn get-potoos [req]
  (let [data (db/find-all-potoos (:db-conn req))]
    (log/info "Getting all potoos from" (:remote-addr req))
    (resp/response data)))

(defn create-potoo [req]
  (let [{:keys [:id :username]} (-> (:identity req))
        text (-> req :body :text)
        _ (db/create-potoo (:db-conn req) id text)]
    (resp/response {:text text :name username})))

(defn create-user [req]
  (let [conn (:db-conn req)
        {:keys [username password]} (:body req)
        password (encrypt password)
        _ (db/create-user conn username password)
        user (db/find-user-by-username conn username)]
    (-> (resp/response {:name username})
        (assoc :session (assoc (:session req) :identity user)))))

(defn create-session [req]
  (let [conn (:db-conn req)
        {:keys [username password]} (-> req :body)
        user (db/find-user-by-username conn username)]
    (if (hashers/check password (:password user))
      (-> (resp/response {:name (:username user)})
          (assoc :session (assoc (:session req) :identity user)))
      (unauthorized))))

(defn delete-session [{session :session}]
  (let [updated-session (assoc session :identity nil)]
    (-> (resp/response nil)
        (assoc :session updated-session))))

(defn get-all-data [req]
  (resp/response {:potoos (db/find-all-potoos (:db-conn req))
                  :user   (-> req :identity :username)}))

(defn index-handler [_]
  (resp/file-response "index.html" {:root "resources/public"}))

;; Routes

(def routes
  ["/" {""     index-handler
        "api" {"" {:get get-all-data}
               "/potoos" {:get get-potoos
                          :post create-potoo}
               "/users" {:post create-user}
                "/sessions" {:post create-session
                             :delete delete-session}}}])

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

(defrecord WebServer [opts container postgres-connection]
  component/Lifecycle
  (start [component]
    (log/info "Starting web server with params:" opts)
    (let [req-handler (potoo-handler (:db-spec postgres-connection))
          container (run-jetty req-handler opts)]
      (assoc component :container container)))
  (stop [component]
    (log/info "Stopping web server")
    (.stop container)
    component))

(defn new-server [opts]
  (WebServer. opts nil nil))
