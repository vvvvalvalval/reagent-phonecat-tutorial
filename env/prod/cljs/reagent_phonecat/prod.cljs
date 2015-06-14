(ns reagent-phonecat.prod
  (:require [reagent-phonecat.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
