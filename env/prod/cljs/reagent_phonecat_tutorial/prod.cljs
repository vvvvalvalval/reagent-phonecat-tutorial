(ns reagent-phonecat-tutorial.prod
  (:require [reagent-phonecat-tutorial.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
