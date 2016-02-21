(ns ^:figwheel-always potoo.core
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]))

(defonce app-state
  (r/atom {:potoos []
           :user nil}))

(defn potoo [p]
  (let [{:keys [text name date]} p]
    [:li
     [:span (str text ", " name ", " date)]]))

(defn create-potoo [text]
  (POST "/api/potoos" {:params {:text text}
                       :keywords? true
                       :format :json
                       :response-format :json
                       :handler #(swap! app-state update-in [:potoos] conj %)}))

(defn login-form [name pass]
  (if-let [user (:user @app-state)]
    [:div
     [:span (str "Welcome " user)]
     [:input {:type "button"
              :value "Logout"
              :on-click #(swap! app-state assoc :user nil)}]]
    [:form
     [:div
      [:label "Username"]
      [:input {:type      "text" :value @name
               :on-change #(reset! name (-> % .-target .-value))}]]
     [:div
      [:label "Password"]
      [:input {:type      "password" :value @pass
               :on-change #(reset! pass (-> % .-target .-value))}]]
     [:div
      [:input {:type     "button"
               :value    "Login"
               :on-click (fn []
                           (POST "/api/sessions"
                                 {:params          {:username @name :password @pass}
                                  :keywords?       true
                                  :format          :json
                                  :response-format :json
                                  :handler         #(swap! app-state assoc :user (:name %))}))}]]]))

(defn potoo-form [text]
  (if (:user @app-state)
    [:div
     [:input {:type      "text" :value @text
              :on-change #(reset! text (-> % .-target .-value))}]
     [:input {:type     "button"
              :value    "Submit"
              :disabled (-> @text str/trim empty?)
              :on-click #(create-potoo @text)}]]
    [:p "Please login to post a potoo"]))

(defn potoo-list []
  [:div
   [:ul
    (for [p (sort-by :date (:potoos @app-state))]
      ^{:key p} [potoo p])]])

(def potoo-list-initial
  (with-meta potoo-list
    {:get-initial-state
     (fn [_]
       (GET "/api/potoos" {:keywords? true
                           :response-format :json
                           :handler #(swap! app-state assoc :potoos %)}))}))

(defn potoo-wrapper []
  (let [text (r/atom "")
        name (r/atom "")
        pass (r/atom "")]
    [:div
     [:h1 "Potooooooos!"]
     [login-form name pass]
     [potoo-form text]
     [potoo-list-initial]]))

(defn ^:export run []
  (r/render
    [potoo-wrapper]
    (js/document.getElementById "app")))

(run)

