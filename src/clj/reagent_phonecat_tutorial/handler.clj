(ns reagent-phonecat-tutorial.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [environ.core :refer [env]]))

(def home-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css (str "/webjars/bootstrap/3.1.0/css/bootstrap" (if (env :dev) "" ".min") ".css")) ;; including Twitter Bootstrap stylesheets
     (include-css (if (env :dev) "css/site.css" "css/site.min.css")) ;; our own stylesheets
     ]
    [:body
     [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]]
     (include-js "js/app.js") 
     ]]))

(defroutes routes
  (GET "/" [] home-page)
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (-> routes 
                  (wrap-resource "/META-INF/resources")
                  (wrap-defaults site-defaults))]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))
