(ns reagent-phonecat.core
    (:require [reagent.core :as rg])
    )

(enable-console-print!)

(defn say-hello! "Greets `name`, or the world if no name specified.
Try and call this function from the ClojureScript REPL."
  [& [name]]
  (print "Hello," (or name "World") "!"))

(def static-content "Some sample, statically defined DOM content."
     [:ul#phones-list
      [:li.phone-item 
       [:span "Nexus S"]
       [:p "Fast just got faster with Nexus S"]]
      [:li {:class "phone-item"} 
       [:span "Motorola XOOM™ with Wi-Fi"]
       [:p "The Next, Next Generation tablet."]]
      ])

(defn mount-root "Creates the application view and injects ('mounts') it into the root element." 
  []
  (rg/render 
    static-content
    (.getElementById js/document "app")))

(defn init! []
  (mount-root))
