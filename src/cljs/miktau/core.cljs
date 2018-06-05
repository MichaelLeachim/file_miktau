(ns miktau.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            
            [miktau.core-views :as miktau-core-views]
            [miktau.config :as config]))

;; (enable-console-print!)


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [miktau-core-views/main]
                  (.getElementById js/document "container")))

(defn render []
  (re-frame/dispatch-sync [:cloud/init-page #{}])
  
  ;; (re-frame/dispatch-sync [:nodes/init-page #{}  #{:bibliostore}])
  
  ;; (re-frame/dispatch-sync [:nodes/init-page #{}  #{}])
  ;; (re-frame/dispatch-sync [:edit-nodes/init-page #{"*"}  #{:bibliostore}])
  

  (dev-setup)
  (mount-root))
