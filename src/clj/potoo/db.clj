(ns potoo.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [jdbc.pool.c3p0 :as pool]
    [migratus.core :as migratus]
    [taoensso.timbre :as log]
    [com.stuartsierra.component :as component]
    [clojure.string :as str])
  (:import (java.net URI)))

;; =============
;; Queries

(defn find-all-users [db]
  (jdbc/query db ["select * from users"]))

;; =============
;; DB connection

(defn- get-user-and-password [uri]
  (if (nil? (.getUserInfo uri))
    nil (str/split (.getUserInfo uri) #":")))

(defn- get-db-spec [uri]
  {:classname        "org.postgresql.Driver"
   :subprotocol      "postgresql"
   :user             (get (get-user-and-password uri) 0)
   :password         (get (get-user-and-password uri) 1)
   :subname          (if (= -1 (.getPort uri))
                       (format "//%s%s" (.getHost uri) (.getPath uri))
                       (format "//%s:%s%s" (.getHost uri) (.getPort uri) (.getPath uri)))})

(defn- make-db-spec [uri]
  (pool/make-datasource-spec (log/spy (get-db-spec uri))))

(defn- migrate-db [uri]
  (let [config {:store :database
                :db    (log/spy (get-db-spec uri))}]
    (log/info "Applying migrations")
    (migratus/migrate config)))

(defn- get-db-uri [uri]
  (URI.
    (or
      (System/getenv "DATABASE_URL")
      uri)))

(defrecord PostgresDatabase [uri]
  component/Lifecycle
  (start [component]
    (log/info "Connecting to Postgres.")
    (let [uri (get-db-uri uri)
          db (make-db-spec uri)]
      (migrate-db uri)
      (assoc component :db-spec db)))
  (stop [component]
    (log/info "Closing the connection to Postgres")
    (.close (:db-spec component))
    component))

(defn new-database [uri]
  (PostgresDatabase. uri))

(comment
  (let [db (new-database "postgres://potoo:potoo@localhost:5432/potoo")
        db (:db-spec (component/start db))]
    (find-all-users db)))