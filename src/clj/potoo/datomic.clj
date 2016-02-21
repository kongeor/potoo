(ns potoo.datomic
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clj-time.coerce :as c]
            [clj-time.core :as t])
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

(defn find-user-id-by-name-and-pass [conn username password]
  (ffirst
    (d/q '[:find ?uid
           :in $ ?username ?password
           :where [?uid :user/name ?username]
                  [?uid :user/password ?password]]
         (d/db conn)
         username
         password)))

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

;; Fixtures

(defn create-fixture-data [conn]
  (let [date (c/to-date (t/date-time 1986 10 14 4 3 27 456))]
    (create-user conn "Morty" "Morty")
    (create-user conn "Rick" "Rick")
    (create-user conn "Jerry" "Jerry")
    (create-potoo conn "Ohh yea, you gotta get schwifty." "Rick" date)
    (create-potoo conn "Wubbalubbadubdub" "Rick" date)
    (create-potoo conn "Don't be trippin dog we got you" "Morty" date)
    (create-potoo conn "It probably has space aids" "Jerry" date)))

;; Component

(defrecord DatomicDatabase [uri schema create-fixtures db-conn]
  component/Lifecycle
  (start [component]
    (log/info "Creating database connection to" uri)
    (d/create-database uri)
    (let [c (d/connect uri)]
      @(d/transact c schema)
      (when create-fixtures
        (create-fixture-data c))
      (assoc component :db-conn c)))
  (stop [component]
    component))

(defn new-database [db-uri]
  (DatomicDatabase.
    db-uri
    (first (Util/readAll (io/reader (io/resource "datomic/schema.edn"))))
    true
    nil))

(defn new-empty-database [db-uri]
  (DatomicDatabase.
    db-uri
    (first (Util/readAll (io/reader (io/resource "datomic/schema.edn"))))
    false
    nil))

(comment
  (let [datomic (new-database "datomic:mem://localhost:4334/potoos")
        conn (-> (.start datomic) :db-conn)]
       (find-user-id-by-name-and-pass conn "Rick" "Rick")))
