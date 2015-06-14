(ns reagent-phonecat.core
    (:require [reagent.core :as rg])
    )

(enable-console-print!)

(defn say-hello! "Greets `name`, or the world if no name specified.
Try and call this function from the ClojureScript REPL."
  [& [name]]
  (print "Hello," (or name "World") "!"))

(defn mount-root "Creates the application view and injects ('mounts') it into the root element." 
  []
  (rg/render 
    [:p "Nothing here " (str "yet" "!")]
    (.getElementById js/document "app")))

(defn init! []
  (mount-root))
