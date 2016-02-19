(ns ^:figwheel-always potoo.core
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]))

(defonce app-state
  (r/atom {:potoos []}))

(defn potoo [p]
  (let [{:keys [text name date]} p]
    [:li
     [:span (str text ", " name ", " date)]]))

(defn potoo-list []
  [:div
   [:h1 "Potooooooos!"]
   [:ul
    (for [p (:potoos @app-state)]
      ^{:key p} [potoo p])]])

(def potoo-list-initial
  (with-meta potoo-list
    {:get-initial-state
     (fn [_]
       (GET "/api/potoos" {:keywords? true
                           :response-format :json
                           :handler #(swap! app-state assoc :potoos %)}))}))

(defn ^:export run []
  (r/render
    [potoo-list-initial]
    (js/document.getElementById "app")))

(run)

;; demo

(comment
  (defn cp [text]
    (let [name "Mr. Meeseeks"
          date (str (js/Date.))
          potoo {:key "zxc" :text text :name name :date date}]
      (swap! app-state update-in [:potoos] conj potoo))))

