(ns potoo.system
  (:require [com.stuartsierra.component :as component]
            [potoo.datomic :as db]
            [potoo.server :as server]
            [taoensso.timbre :as log])
  (:gen-class))

(defn dev []
  (in-ns 'dev))

(defn new-system [config-options]
  (let [{:keys [db-uri]} config-options
        web-opts (select-keys config-options [:port :join?])]
    (log/info "Bootstrapping system")
    (component/system-map
      :db (db/new-database db-uri)
      :webserver
      (component/using
        (server/new-server web-opts)
        {:datomic-connection :db}))))

(defn -main [& args]
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "8081"))]
    (.start
      (new-system
        {:db-uri "datomic:mem://localhost:4334/potoos"
         :port   port
         :join?  true}))))
