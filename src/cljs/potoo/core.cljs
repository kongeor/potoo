(ns ^:figwheel-always potoo.core
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [ajax.core :refer [GET POST DELETE]]))

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
     [:input {:type     "button"
              :value    "Logout"
              :on-click (fn [] (DELETE "/api/sessions"
                                       {:handler #(swap! app-state assoc :user nil)}))}]]
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
     [:h3 "Potoo it!"]
     [:input {:type      "text" :value @text
              :on-change #(reset! text (-> % .-target .-value))}]
     [:input {:type     "button"
              :value    "Submit"
              :disabled (-> @text str/trim empty?)
              :on-click #(create-potoo @text)}]]
    [:p "Please login to post a potoo"]))

(defn potoos-for-user [user potoos]
  [:div
   [:h3 user]
   [:ul
    (for [p (sort-by :date potoos)]
      ^{:key p} [potoo p])]])

(defn potoo-list []
  [:div
   (for [[name potoos] (seq (group-by :name (:potoos @app-state)))]
     [potoos-for-user name potoos])])

(def potoo-list-initial
  (with-meta potoo-list
    {:get-initial-state
     (fn [_]
       (GET "/api" {:keywords? true
                    :response-format :json
                    :handler #(reset! app-state %)}))}))

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

