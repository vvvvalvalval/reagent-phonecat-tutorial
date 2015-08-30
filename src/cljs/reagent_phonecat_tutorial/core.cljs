(ns reagent-phonecat.core
  (:import [goog History])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [reagent-phonecat.core :refer [<? go-safe spy]])
  (:require [reagent.core :as rg]
            [clojure.string :as str]
            [ajax.core :as ajx]
            [bidi.bidi :as b :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [cljs.core.async :as a])
  )

(enable-console-print!)

(defn say-hello! "Greets `name`, or the world if no name specified.
Try and call this function from the ClojureScript REPL."
  [& [name]]
  (print "Hello," (or name "World") "!"))

;; --------------------------------------------
;; Utility data 

(defn throw-err "Accept a value and returns it, unless it is an Error, in which case it throws it."
  [v]
  (if (instance? js/Error v) (throw v) v))

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

(comment
  (try (throw err) (catch :default e (println (ex-data e))))
  )

;; --------------------------------------------
;; State

(def state "Reagent atom that holds our global application state." 
  (rg/atom {:phones []
            :search ""
            :order-prop :name
            
            :phone-by-id {}
            
            :navigation {:page :phones ;; can be any one of #{:phones :phone}
                         :params {}}
            }))

(def navigational-state (rg/cursor state [:navigation]))


(def order-prop-state (rg/cursor state [:order-prop]))
    
    
(defn update-search [state new-search]
  (assoc state :search new-search))


;; --------------------------------------------
;; Server communication

(defn ajax-call "Accept a cljs-ajax request map, and returns a channel which will contain the response, or an Error if the response is an error."
  [{:keys [method uri] :as opts}]
  (let [=resp= (a/chan)]
    (ajx/ajax-request (assoc opts
                        :handler (fn [[ok r :as data]]
                                   (if ok
                                     (a/put! =resp= r)
                                     (a/put! =resp= (ex-info "AJAX error" {:request opts :response r}))
                                     ))
                        ))
    =resp=))

(def ajax-defaults "Basic options for the response format"
  {:format (ajx/json-request-format)
   :response-format (ajx/json-response-format {:keywords? true})})

(defn fetch-phones-list []
  (ajax-call (assoc ajax-defaults
               :method :get :uri "/phones/phones.json")))

(defn fetch-phone-details [phone-id]
  (ajax-call (assoc ajax-defaults
               :method :get :uri (str "/phones/" phone-id ".json"))))

(defmulti load-page-data "Loads data for a page and returns a function with which to swap! the application state"
  (fn [page params] page))

(defmethod load-page-data :phones
  [_ _] (go-safe
          (let [phones (<? (fetch-phones-list))]
            #(assoc % :phones phones)
            )))

(defmethod load-page-data :phone
  [_ {:keys [phone-id]}] (go-safe
                           (let [phone-details (<? (fetch-phone-details phone-id))]
                             #(assoc-in % [:phone-by-id phone-id] phone-details))))

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

(def =path-changes= "A channel which will output the new value of the path when the URL changes"
  (a/chan (a/sliding-buffer 1) (comp (map (fn [event] (.-token event))) (dedupe))))

(defn hook-browser-navigation! "Watches the path in the URL and puts change events to the =path-changes= channel."
  []
  (doto h
    (events/listen
      EventType/NAVIGATE (fn [event] (a/put! =path-changes= event)))
    (.setEnabled true)))

(defn listen-to-paths-changes! "Listen to changes in the path, resolving the new page and fetching its data, or falling back to last page if an error occurred"
  [routes]
  (go (loop [last-path "/phones"] ;; the last page for which routing was successful, we'll fall back to it if something goes wrong.
        (when-let [next-path (a/<! =path-changes=)]
          (let [{:keys [page params] :as nav} (url-to-nav routes next-path)
                new-last-path (cond
                                (nil? page) (do (.replaceToken h last-path) ;; route does not exist, fall back to last page
                                                last-path)
                                :else (try
                                        (let [change-data (<? (load-page-data page params))]
                                          (swap! state change-data)
                                          (reset! navigational-state nav) ;; success, the data has loaded, we can set the navigational state to its new value
                                          next-path)
                                        (catch js/Object err ;; an error occurred, abort page change
                                          (do (.replaceToken h last-path) ;; route does not exist, fall back to last page
                                              last-path)))
                                )]
            (recur new-last-path)
            )))))

;; --------------------------------------------
;; View components

(declare ;; here we declare our components to define their in an order that feels natural.  
  top-cpnt 
    phones-list-page
      search-cpnt
      order-prop-select
      phones-list 
        phone-item
    phone-page
      phone-detail-cpnt
        phone-spec-cpnt
        checkmark)

(defn- find-phone-by-id [phones phone-id]
  (->> phones (filter #(= (:id %) phone-id)) first))

(defn top-cpnt []
  (let [{:keys [page params]} @navigational-state]
    (case page
      :phones [phones-list-page]
      :phone (let [phone-id (:phone-id params)]
               [phone-page phone-id])
      [:div "This page does not exist"]
      )))

(defn phones-list-page []
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

(defn phone-page [phone-id]
  (let [phone-cursor (rg/cursor state [:phone-by-id phone-id])
        phone @phone-cursor]
    (cond 
      phone ^{:key phone-id} [phone-detail-cpnt phone] 
      :not-loaded-yet [:div])))

(defn phone-detail-cpnt [phone]
  (let [{:keys [images]} phone
        local-state (rg/atom {:main-image (first images)})]
    (fn [phone]
      (let [{:keys [images name description availability additionalFeatures]
         {:keys [ram flash]} :storage
         {:keys [type talkTime standbyTime]} :battery
         {:keys [cell wifi bluetooth infrared gps]} :connectivity
         {:keys [os ui]} :android
         {:keys [dimensions weight]} :sizeAndWeight
         {:keys [screenSize screenResolution touchScreen]} :display
         {:keys [cpu usb audioJack fmRadio accelerometer]} :hardware
         {:keys [primary features]} :camera
         } phone]
    [:div
     [:img.phone {:src (:main-image @local-state)}]
     [:h1 name]
     [:p description]

     [:ul.phone-thumbs
      (for [img images]
        [:li [:img {:src img :on-click #(swap! local-state assoc :main-image img)}]])]
     
     [:ul.specs
      [phone-spec-cpnt "Availability and Networks" [(cons "Availability" availability)]]
      [phone-spec-cpnt "Battery" [["Type" type] ["Talk Time" talkTime] ["Standby time (max)" standbyTime]]]
      [phone-spec-cpnt "Storage and Memory" [["RAM" ram] ["Internal Storage" flash]]]
      [phone-spec-cpnt "Connectivity" [["Network Support" cell] ["WiFi" wifi] ["Bluetooth" bluetooth] ["Infrared" (checkmark infrared)] ["GPS" (checkmark gps)]]]
      [phone-spec-cpnt "Android" [["OS Version" os] ["UI" ui]]]
      [phone-spec-cpnt "Size and Weight" [(cons "Dimensions" dimensions) ["Weight" weight]]]
      [phone-spec-cpnt "Display" [["Screen size" screenSize] ["Screen resolution" screenResolution] ["Touch screen" (checkmark touchScreen)]]]
      [phone-spec-cpnt "Hardware" [["CPU" cpu] ["USB" usb] ["Audio / headphone jack" audioJack] ["FM Radio" (checkmark fmRadio)] ["Accelerometer" (checkmark accelerometer)]]]
      [phone-spec-cpnt "Camera" [["Primary" primary] ["Features" (str/join ", " features)]]]
      [:li
       [:span "Additional Features"]
       [:dd additionalFeatures]]
      ]
     ]))))

(defn phone-spec-cpnt [title kvs]
  [:li
   [:span title]
   [:dl (->> kvs (mapcat (fn [[t & ds]]
                           [[:dt t] (for [d ds][:dd d])]
                           )))]])

(defn checkmark [input] (if input \u2713 \u2718))

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
  [{:keys [name snippet id imageUrl] :as phone}]
  (let [phone-page-href (str "#/phones/" id)]
    [:li {:class "thumbnail"}
     [:a.thumb {:href phone-page-href} [:img {:src imageUrl}]]
     [:a {:href phone-page-href} name]
     [:p snippet]]))

(defn mount-root "Creates the application view and injects ('mounts') it into the root element." 
  []
  (rg/render 
    [top-cpnt]
    (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (listen-to-paths-changes! routes)
  (mount-root))
