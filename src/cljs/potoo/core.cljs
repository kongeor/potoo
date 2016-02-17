(ns ^:figwheel-always potoo.core
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]))

(def app-state
  (r/atom
    {:potoos
     [{:key 1 :text "Ow man" :author "Morty"}
      {:key 2 :text "Don't judge" :author "Rick"}]}))

(defn create-potoo [text]
  (let [name "Mr. Meeseeks"
        date (str (js/Date.))
        potoo {:text text :name name :date date}]
    (swap! app-state update-in [:potoos] conj potoo)))

(defn potoo [p]
  [:li
   [:span (:text p)]
   [:span ", "]
   [:span (:name p)]
   [:span ", "]
   [:span (:date p)]])

(defn potoo-list []
  [:div
   [:h1 "Potooooooos!"]
   [:ul
    (for [p (:potoos @app-state)]
      [potoo p])]])

(defn start []
  (r/render-component
    [potoo-list]
    (.getElementById js/document "app")))

(start)

(defn handler [response]
  (swap! app-state assoc :potoos response))

(GET "/api/potoos" {:keywords? true
                    :response-format :json
                    :handler handler})

