(ns ^:figwheel-always potoo.core
  (:require [reagent.core :as r]
            [clojure.string :as str]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [ajax.core :refer [GET POST DELETE]]))

(defonce app-state
  (r/atom {:potoos []
           :user nil}))

(defn potoo [p]
  (let [{:keys [text name created_at]} p]
    [:li
     [:span (str text ", " name ", " created_at)]]))

(defn create-potoo [text]
  (POST "/api/potoos" {:params {:text text}
                       :keywords? true
                       :format :json
                       :response-format :json
                       :handler #(swap! app-state update-in [:potoos] conj %)}))

(defn validate-register [data errors]
  (let [val (b/validate data
                        :username v/required
                        :password v/required)]
    (reset! errors (first val))))

(defn register [register-data errors]
  (when-not (validate-register register-data errors)
    (POST "/api/users"
          {:params          register-data
           :keywords?       true
           :format          :json
           :response-format :json
           :handler         #(swap! app-state assoc :user (:username register-data))})))

(defn register-form [register-data errors]
  [:div
   [:h3 "Register"]
   (when @errors
     [:ul
      (for [e (vals @errors)]
        [:li {:style {:color "red"}} (first e)])])
   [:form
    [:div
     [:label "Username"]
     [:input {:class     "u-full-width"
              :type      "text" :value (:username @register-data)
              :on-change #(swap! register-data assoc :username (-> % .-target .-value))}]]
    [:div
     [:label "Password"]
     [:input {:class "u-full-width"
              :type  "password" :value (:password @register-data)
              :on-change #(swap! register-data assoc :password (-> % .-target .-value))}]]
    [:div
     [:input {:class    "button-primary"
              :type     "button"
              :value    "Register"
              :on-click #(register @register-data errors)
              }]]]])

(defn generic-form [name pass action handler]
  [:div
   [:h3 action]
   [:form
    [:div
     [:label "Username"]
     [:input {:class "u-full-width"
              :type      "text" :value @name
              :on-change #(reset! name (-> % .-target .-value))}]]
    [:div
     [:label "Password"]
     [:input {:class "u-full-width"
              :type      "password" :value @pass
              :on-change #(reset! pass (-> % .-target .-value))}]]
    [:div
     [:input {:class "button-primary"
              :type     "button"
              :value    action
              :on-click handler}]]]])

(defn register-login-form []
  (if-let [user (:user @app-state)]
    [:div
     [:span (str "Welcome " user)]
     [:input {:type     "button"
              :value    "Logout"
              :on-click (fn [] (DELETE "/api/sessions"
                                       {:handler #(swap! app-state assoc :user nil)}))}]]
    (let [lname (r/atom "")
          lpass (r/atom "")
          register-data (r/atom {})
          rerrors (r/atom nil)]
      [:div.row
       [:div.one-half.column
        [register-form register-data rerrors]]
       [:div.one-half.column
        [generic-form lname lpass "Login" (fn []
                                            (POST "/api/sessions"
                                                  {:params          {:username @lname :password @lpass}
                                                   :keywords?       true
                                                   :format          :json
                                                   :response-format :json
                                                   :handler         #(swap! app-state assoc :user (:name %))}))]]])))

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
     ^{:key name} [potoos-for-user name potoos])])

(def potoo-list-initial
  (with-meta potoo-list
    {:get-initial-state
     (fn [_]
       (GET "/api" {:keywords? true
                    :response-format :json
                    :handler #(reset! app-state %)}))}))

(defn potoo-wrapper []
  (let [text (r/atom "")]
    [:div.container
     [:h1 "Potooooooos!"]
     [register-login-form]
     [potoo-form text]
     [potoo-list-initial]]))

(defn ^:export run []
  (r/render
    [potoo-wrapper]
    (js/document.getElementById "app")))

(run)

