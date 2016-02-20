(ns ^:figwheel-always potoo.core
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]))

(defonce app-state
  (r/atom {:potoos []}))

(defn potoo [p]
  (let [{:keys [text name date]} p]
    [:li
     [:span (str text ", " name ", " date)]]))

(defn create-potoo [text]
    (let [name "Mr. Meeseeks"
          date (str (js/Date.))
          potoo {:key "zxc" :text text :name name :date date}]
      (swap! app-state update-in [:potoos] conj potoo)
      (POST "/api/potoos" {:params {:text text}
                           :keywords? true
                           :format :json
                           :response-format :json
                           :handler #(js/console.log %)})))

(defn potoo-form [text]
  (let [text-empty? (-> @text str/trim empty?)]
    [:div
     [:input {:type      "text" :value @text
              :on-change #(reset! text (-> % .-target .-value))}]
     [:input {:type     "button" :value "Submit" :disabled text-empty?
              :on-click #(create-potoo @text)}]]))

(defn potoo-list []
  [:div
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

(defn potoo-wrapper []
  (let [text (r/atom "")]
    [:div
     [:h1 "Potooooooos!"]
     [potoo-form text]
     [potoo-list-initial]]))

(defn ^:export run []
  (r/render
    [potoo-wrapper]
    (js/document.getElementById "app")))

(run)

