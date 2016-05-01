(ns potoo.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [jdbc.pool.c3p0 :as pool]
    [migratus.core :as migratus]
    [honeysql.core :as sql]
    [honeysql.helpers :refer :all]
    [taoensso.timbre :as log]
    [com.stuartsierra.component :as component]
    [clojure.string :as str])
  (:import (java.net URI)))

;; =============
;; Utils

(defmacro with-sql-profile [func db sql]
  `(let [start# (. System (nanoTime))
         _#      (log/debug ~sql)
         ret#   (~func ~db ~sql)
         time#  (/ (double (- (. System (nanoTime)) start#)) 1000000.0)]
     (log/debug ~sql "- Query Time:" time# "msecs")
     ret#))

;; =============
;; Queries

(defn find-all-users [db]
  (jdbc/query db ["select * from users"]))

(defn find-all-potoos [db]
  (let [sql (-> (select :*)
                (from :potoos)
                sql/format)]
    (with-sql-profile jdbc/query db sql)))

(defn find-potoos-for-user [db user_id]
  (let [sql (-> (select :*)
                (from :potoos)
                (where [:= :user_id user_id])
                sql/format)]
    (with-query db sql)))

(defn create-user [db username password]
  (let [sql (-> (insert-into :users)
                (columns :username :password)
                (values [[username password]])
                sql/format)]
    (log/debug sql)
    (jdbc/execute! db sql)))

(defn create-potoo [db user_id text]
  (let [sql (-> (insert-into :potoos)
                (columns :user_id :text)
                (values [[user_id text]])
                sql/format)]
    (log/debug sql)
    (jdbc/execute! db sql)))

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
  (def db (-> (new-database "postgres://potoo:potoo@localhost:5432/potoo")
              component/start
              :db-spec)))

(comment
  (create-user db "saki" "saki")
  (find-all-users db)
  (create-potoo db 2 "quux")
  (find-all-potoos db)
  (find-potoos-for-user db 1))