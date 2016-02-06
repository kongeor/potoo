(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [potoo.system :as system]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly
                    (system/dev-system
                      {:db-uri   "datomic:mem://localhost:4334/potoos"
                       :web-port 8081}))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))