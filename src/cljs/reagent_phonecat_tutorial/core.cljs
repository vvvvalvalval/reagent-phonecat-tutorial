(ns reagent-phonecat.core
    (:require [reagent.core :as rg]
              [clojure.string :as str]
              [ajax.core :as ajx])
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

(def state "Reagent atom that holds our global application state." 
  (rg/atom {:phones []
            :search ""
            :order-prop :name
            }))


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
;; View components

(declare ;; here we declare our components to define their in an order that feels natural.  
  top-cpnt 
    search-cpnt
    order-prop-select
    phones-list 
      phone-item)

(defn top-cpnt []
  (let [{:keys [phones search]} @state]
    [:div.container-fluid
     [:div.row
      [:div.col-md-2 
       [search-cpnt search]
       [:br]
       "Sort by:"
       [order-prop-select]]
      [:div.col-md-8 [phones-list phones search @order-prop-state]]
      ]]))

(defn search-cpnt [search]
  [:span 
   "Search: "
   [:input {:type "text" 
            :value search
            :on-change (fn [e] (swap! state update-search (-> e .-target .-value)))}]])

(defn order-prop-select []
  [:select {:value @order-prop-state
            :on-change #(reset! order-prop-state (-> % .-target .-value keyword))}
   [:option {:value :name} "Alphabetical"]
   [:option {:value :age} "Newest"]
   ])

(defn phones-list "An unordered list of phones" 
  [phones-list search order-prop]
  [:ul.phones
   (for [phone (->> phones-list 
                 (filter #(matches-search? search %))
                 (sort-by order-prop))]
     ^{:key (:name phone)} [phone-item phone]
     )])

(defn phone-item "An phone item component"
  [{:keys [name description] :as phone}]
  [:li.phone-item 
   [:span name]
   [:p description]])

(defn mount-root "Creates the application view and injects ('mounts') it into the root element." 
  []
  (rg/render 
    [top-cpnt]
    (.getElementById js/document "app")))

(defn init! []
  (load-phones! state)
  (mount-root))
