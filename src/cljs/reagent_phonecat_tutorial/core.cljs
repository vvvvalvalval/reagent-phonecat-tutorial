(ns reagent-phonecat.core
    (:require [reagent.core :as rg])
    )

(enable-console-print!)

(defn say-hello! "Greets `name`, or the world if no name specified.
Try and call this function from the ClojureScript REPL."
  [& [name]]
  (print "Hello," (or name "World") "!"))

;; --------------------------------------------
;; Application data

(def hardcoded-phones-data [{:name "Nexus S" 
                             :description "Fast just got faster with Nexus S"}
                            {:name "Motorola XOOM™ with Wi-Fi" 
                             :description "The Next, Next Generation tablet."}])

;; --------------------------------------------
;; View components

(declare ;; here we declare our components to define their in an order that feels natural.  
  <phones-list>
    <phone-item>)

(defn <phones-list> "An unordered list of phones"
  [phones-list]
  [:div.container-fluid
   [:ul
    (for [phone phones-list]
      ^{:key (:name phone)} [<phone-item> phone]
      )]])

(defn <phone-item> "An phone item component"
  [{:keys [name description] :as phone}]
  [:li.phone-item 
   [:span name]
   [:p description]])

(defn mount-root "Creates the application view and injects ('mounts') it into the root element." 
  []
  (rg/render 
    [<phones-list> hardcoded-phones-data]
    (.getElementById js/document "app")))

(defn init! []
  (mount-root))
