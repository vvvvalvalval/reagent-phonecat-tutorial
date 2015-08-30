(ns reagent-phonecat.core
  (:import [goog History])  
  (:require [reagent.core :as rg]
            [clojure.string :as str]
            [ajax.core :as ajx]
            [bidi.bidi :as b :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
    )

(enable-console-print!)

(defn say-hello! "Greets `name`, or the world if no name specified.
Try and call this function from the ClojureScript REPL."
  [& [name]]
  (print "Hello," (or name "World") "!"))

;; --------------------------------------------
;; Application data

(def hardcoded-phones-data [{:name "Nexus S"
                             :description "Fast just got faster with Nexus S."
                             :age 1}
                            {:name "Motorola XOOM™ with Wi-Fi"
                             :description "The Next, Next Generation tablet."
                             :age 2}
                            {:name "MOTOROLA XOOM™"
                             :description "The Next, Next Generation tablet."
                             :age 3}])

;; --------------------------------------------
;; Search logic

(defn matches-search? "Determines if a phone item matches a text query."
  [search data]
  (let [qp (-> search (or "") str/lower-case re-pattern)]
    (->> (vals data)
         (filter string?) (map str/lower-case)
         (some #(re-find qp %))
         )))

;; --------------------------------------------
;; State

(defonce state
  (rg/atom {:phones []
            :search ""
            :order-prop :name
            
            :navigation {:page :phones ;; can be any one of #{:phones :phone}
                         :param {}}
            }))

(def navigational-state (rg/cursor state [:navigation]))


(def order-prop-state (rg/cursor state [:order-prop]))
    
    
(defn update-search [state new-search]
  (assoc state :search new-search))


;; --------------------------------------------
;; Server communication

(defn load-phones! "Fetches the list of phones from the server and updates the state atom with it" 
  [state]
  (ajx/GET "/phones/phones.json"
           {:handler (fn [phones] (swap! state assoc :phones phones))
            :error-handler (fn [details] (.warn js/console (str "Failed to refresh phones from server: " details)))
            :response-format :json, :keywords? true}))


;; --------------------------------------------
;; Routing 

;; we declare the routes with a tree-ish data structure which leaves identify our pages
(def routes 
  ["/phones" {"" :phones
              ["/" :phone-id] :phone}])

;; then we leverage bidi functions to match against this data structure
(defn url-to-nav [routes path]
  (let [{:keys [handler route-params]} (b/match-route routes path)]
    {:page handler :params route-params}))

(defn nav-to-url [routes {:keys [page params]}]
  (apply b/path-for routes page (->> params seq flatten)))

(comment 
  (url-to-nav routes "/phones")
  => {:page :phones :params nil}
  (nav-to-url routes {:page :phones})
  => "/phones" 
  
  (url-to-nav routes "/phones/motorola-xoom")
  => {:page :phone :params {:phone-id "motorola-xoom"}}
  (nav-to-url routes {:page :phone :params {:phone-id "motorola-xoom"}})
  => "/phones/motorola-xoom"
  )


(def h (History.))

(defn navigate-to! [routes nav]
  (.setToken h (nav-to-url routes nav)))

(defn hook-browser-navigation! "Listen to navigation events and dispatches a route change accordingly through secretary."
  [routes]
  (doto h
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (let [path (.-token event)
              {:keys [page params] :as nav} (url-to-nav routes path)]
          (if page
            (reset! navigational-state nav)
            (do
              (.warn js/console (str "No route matches token " path ", redirecting to /phones"))
              (navigate-to! routes {:page :phones}))
            ))
        ))
    (.setEnabled true)))

;; --------------------------------------------
;; View components

(declare ;; here we declare our components to define their in an order that feels natural.  
  <top-cpnt>
    <phones-list-page>
      <search-cpnt>
      <order-prop-select>
      <phones-list>
        <phone-item>
    <phone-page>)

(defn- find-phone-by-id [phones phone-id]
  (->> phones (filter #(= (:id %) phone-id)) first))

(defn <top-cpnt> []
  (let [{:keys [page params]} @navigational-state]
    [:div.container-fluid
     (case page
       :phones [<phones-list-page>]
       :phone (let [phone-id (:phone-id params)
                    phone (find-phone-by-id (:phones @state) phone-id)]
                [<phone-page> phone])
       [:div "This page does not exist"]
       )]))

(defn <phones-list-page> []
  (let [{:keys [phones search]} @state]
    [:div.row
     [:div.col-md-2
      [<search-cpnt> search]
      [:br]
      "Sort by:"
      [<order-prop-select>]]
     [:div.col-md-8 [<phones-list> phones search @order-prop-state]]
     ]))

(defn <search-cpnt> [search]
  [:span
   "Search: "
   [:input {:type "text"
            :value search
            :on-change (fn [e] (swap! state update-search (-> e .-target .-value)))}]])


(defn <phones-list> "An unordered list of phones"
  [phones-list search order-prop]
  [:ul.phones
   (for [phone (->> phones-list
                    (filter #(matches-search? search %))
                    (sort-by order-prop))]
     ^{:key (:name phone)} [<phone-item> phone]
     )])

(defn <phone-item> "An phone item component"
  [{:keys [name snippet id imageUrl] :as phone}]
  (let [phone-page-href (str "#/phones/" id)]
    [:li {:class "thumbnail"}
     [:a.thumb {:href phone-page-href} [:img {:src imageUrl}]]
     [:a {:href phone-page-href} name]
     [:p snippet]]))


(defn <phone-page> [phone]
  [:div "TBD: detail view for " [:span (:id phone)]])

(defn <order-prop-select> []
  [:select {:value @order-prop-state
            :on-change #(reset! order-prop-state (-> % .-target .-value keyword))}
   [:option {:value :name} "Alphabetical"]
   [:option {:value :age} "Newest"]
   ])

(defn mount-root "Creates the application view and injects ('mounts') it into the root element." 
  []
  (rg/render 
    [<top-cpnt>]
    (.getElementById js/document "app")))

(defn init! []
  (load-phones! state)
  (hook-browser-navigation! routes)
  (mount-root))
