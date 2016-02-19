(ns potoo.datomic
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [taoensso.timbre :as log])
  (:import datomic.Util))


;; Queries

(defn find-potoos [conn]
  (d/q '[:find ?text ?name ?created
         :where [?p :potoo/text ?text]
                [?p :potoo/created ?created]
                [?u :user/potoos ?p]
                [?u :user/name ?name]]
       (d/db conn)))

(defn find-potoos-for-user [conn name]
  (d/q '[:find ?text ?created
         :in $ ?name
         :where [?u :user/name ?name]
                [?u :user/potoos ?p]
                [?p :potoo/text ?text]
                [?p :potoo/created ?created]]
       (d/db conn)
       name))

(defn find-user-id [conn name]
  (ffirst
    (d/q '[:find ?uid
           :in $ ?name
           :where [?uid :user/name ?name]]
         (d/db conn)
         name)))

(defn create-user [conn name password]
  @(d/transact conn [{:db/id (d/tempid :db.part/user)
                      :user/name name
                      :user/password password}]))

(defn create-potoo [conn text name date]
  (let [potoo-id (d/tempid :db.part/user)]
    @(d/transact conn [{:db/id potoo-id
                        :potoo/text text
                        :potoo/created date}
                       {:db/id (find-user-id conn name)
                        :user/potoos potoo-id}])))

;; Component

(defrecord DatomicDatabase [uri schema initial-data db-conn]
  component/Lifecycle
  (start [component]
    (log/info "Creating database connection to" uri)
    (d/create-database uri)
    (let [c (d/connect uri)]
      @(d/transact c schema)
      ;@(d/transact c initial-data)
      (assoc component :db-conn c)))
  (stop [component]
    component))

(defn new-database [db-uri]
  (DatomicDatabase.
    db-uri
    (first (Util/readAll (io/reader (io/resource "datomic/schema.edn"))))
    (first (Util/readAll (io/reader (io/resource "datomic/initial.edn"))))
    nil))

(comment
  (let [datomic (new-database "datomic:mem://localhost:4334/potoos")
        conn (-> (.start datomic) :db-conn)]
       (find-potoos conn)))
