(defproject reagent-phonecat-tutorial "0.1.0-SNAPSHOT"
  :description "A Reagent tutorial inspired by AngularJS's Phonecat."
  :url "https://github.com/vvvvalvalval/reagent-phonecat"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs"]

  ;; --------------------------------------------------------------------------------
  ;; Dependencies
  
  :dependencies [;; the Clojure(Script) language
                 [org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 
                 ;; Server-side dependencies
                 [ring-server "0.4.0"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.5"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]
                 [environ "1.0.0"]
                 [prone "0.8.2"]
                 
                 ;; client-side dependencies
                 [cljsjs/react-with-addons "0.13.3-0"]
                 [reagent "0.5.0" :exclusions [cljsjs/react]]
                 [cljs-ajax "0.3.14"]
                 [bidi "1.20.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 ]

  ;; --------------------------------------------------------------------------------
  
  :plugins [[lein-environ "1.0.0"]
            [lein-asset-minifier "0.2.2"]]

  :ring {:handler reagent-phonecat.handler/app
         :uberwar-name "reagent-phonecat.war"}

  :min-lein-version "2.5.0"

  :uberjar-name "reagent-phonecat.jar"

  :main reagent-phonecat.server

  :clean-targets ^{:protect false} [[:cljsbuild :builds :app :compiler :output-dir]
                                    [:cljsbuild :builds :app :compiler :output-to]]

  :minify-assets
  {:assets
    {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                             :compiler {:output-to     "resources/public/js/app.js"
                                        :output-dir    "resources/public/js/out"
                                        :asset-path   "js/out"
                                        :optimizations :none
                                        :pretty-print  true}}}}

  :profiles {:dev {:repl-options {:init-ns reagent-phonecat.repl
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

                   :dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.3.2"]
                                  [weasel "0.6.0"]
                                  [leiningen-core "2.5.1"]
                                  [lein-figwheel "0.3.3"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [pjstadig/humane-test-output "0.7.0"]]

                   :source-paths ["env/dev/clj"]
                   :plugins [[lein-figwheel "0.3.3"]
                             [lein-cljsbuild "1.0.6"]]

                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]

                   :figwheel {:http-server-root "public"
                              :server-port 3449
                              :nrepl-port 7888
                              :css-dirs ["resources/public/css"]
                              :ring-handler reagent-phonecat.handler/app}

                   :env {:dev true}

                   :cljsbuild {:builds {:app {:source-paths ["env/dev/cljs"]
                                              :compiler {:main "reagent-phonecat.dev"
                                                         :source-map true}}
}
}}

             :static-website {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                              :env {:production true}
                              :omit-source true
                              :cljsbuild {:builds {:app
                                                   {:source-paths ["env/prod/cljs"]
                                                    :compiler
                                                    {:optimizations :advanced
                                                     :pretty-print false}}}}}
             :uberjar {:hooks [leiningen.cljsbuild minify-assets.plugin/hooks]
                       :env {:production true}
                       :aot :all
                       :omit-source true
                       :cljsbuild {:jar true
                                   :builds {:app
                                             {:source-paths ["env/prod/cljs"]
                                              :compiler
                                              {:optimizations :advanced
                                               :pretty-print false}}}}}})
