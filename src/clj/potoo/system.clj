(ns potoo.system
  (:require [com.stuartsierra.component :as component]
            [potoo.datomic :as db]
            [potoo.server :as server]))

(defn dev-system [config-options]
  (let [{:keys [db-uri web-port]} config-options]
    (component/system-map
      :db (db/new-database db-uri)
      :webserver
      (component/using
        (server/new-server web-port)
        {:datomic-connection :db}))))
