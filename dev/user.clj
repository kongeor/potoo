(ns user
  (:require [clojure.tools.namespace.repl :refer (refresh)]
            [potoo.system :as system]
            [com.stuartsierra.component :as component]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly
                    (system/new-system
                      {:db-uri "postgres://potoo:potoo@localhost:5432/potoo"
                       :port 8081
                       :join? false}))))

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