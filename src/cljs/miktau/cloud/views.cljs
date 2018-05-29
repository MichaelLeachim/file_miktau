(ns miktau.cloud.views
  (:require [miktau.generic.views-utils :as views-utils]
            [miktau.autocomplete.views :as autocomplete-views]
            [miktau.breadcrumbs.views :as breadcrumbs-views]
            [re-frame.core :as refe]))

(defn e->content
  [e]
  (str
   (aget e "target" "value" )))

(defn general-cloud-tag-item [tag]
  [:a.tag
   {:key  (:key-name tag)
    :href "#"
    :on-click
    (if (:disabled? tag)
      #(refe/dispatch [:cloud/clicked-disabled-cloud-item (tag :key-name)])
      #(refe/dispatch [:cloud/clicked-cloud-item (tag :key-name)]))
    :class
    (str
     (cond (:selected? tag) "selected"
           (:can-select? tag) "can-select"
           (:disabled? tag) "disabled"))
    :style
    {:font-size
     (str  (+ 0.6 (* 2.4  (tag :weighted-size))) "em")}}
   (:name tag) " "])

(defn general-cloud []
  [:div
   (for [tag  @(refe/subscribe [:cloud/cloud])]
     [:span {:key (:key-name tag)}
      [general-cloud-tag-item tag]])])


(defn general-tree []
  [:div
   (for [tag @(refe/subscribe [:cloud/general-tree])]
     [:a.tag
      {:key  (str  (:key-name tag) (:pad-level tag))
       :href "#"
       :on-click
       (if (:disabled? tag)
         #(refe/dispatch [:cloud/clicked-disabled-cloud-item (tag :key-name)])
         #(refe/dispatch [:cloud/clicked-cloud-item (tag :key-name)]))
       :class
       (str
        (cond (:selected? tag) " selected "
              (:can-select? tag) " can-select "
              (:disabled? tag) " disabled ")
        (if (:header? tag)
          " padded-as-button mik-cut-bottom mik-cut-top header-font "
          " ")
        " " (:pad-background-class tag))
       :style
       {:font-size "1em" :display "block"
        :margin-top "3px"
        :margin-bottom "3px"
        :margin-left (str (:pad-level tag) "em")}}
      " "(:name tag) ])])

(defn filter-input []
  [autocomplete-views/filter-input [:cloud/get-app-data] false])

(defn back-button
  []
  [:div {:style {:position "relative"}}
   (if-not @(refe/subscribe [:undos?])
     [:a.unstyled-link.light-gray
      {:style
       {:font-size "5em" :cursor "default"}}
      [views-utils/icon "keyboard_arrow_left"]]
     [:a.unstyled-link.black-clickable
      {:href "#"
       :on-click #(refe/dispatch [:undo])
       :style
       {:font-size "5em"}}
      [views-utils/icon "keyboard_arrow_left"]])])

(defn nodes-selected-view []
  (let [nodes-selection @(refe/subscribe [:cloud/nodes-selection])]
    [:div {:style {:font-size "0.8em" }}
     ;; selected
     [:div.pure-u-1-2 
      [:h2.mik-cut-top.light-gray "Group actions on"]
      [:span "Selected: " [:b (:amount nodes-selection)] " files"]]
     
     ;; node items, narrow down
     [:div.pure-u-1-2.mik-flush-right
      [:button.pure-button.pure-button-primary
       {:on-click #(refe/dispatch (get-in nodes-selection [:narrow-results :on-click]))}
       (get-in nodes-selection [:narrow-results :name])] [:br]
      
      ;; each file action available
      (for [item (:links nodes-selection)]
        ^{:key (:name item)}
        [:span
         (if (:disabled? item)
           [:a.unstyled-link.blue-disaabled (:name item)]
           [:a.unstyled-link.blue-clickable {:href "#" :on-click #(refe/dispatch (:on-click item))} (:name item )])
         [:br]])]]))

(defn main
  []
  [:div.pure-g {:style {:margin-bottom "200px"}}
   ;; header
   [:div.pure-u-1 {:style {:box-shadow "1px 1px 2px 0px gray"}}
    [:div.pure-u-1-24
     [back-button]]
    [:div.pure-u-23-24
     [filter-input]
     [:div.padded-as-button {:style {:font-size "0.7em" :padding-bottom "1em"}}
      [breadcrumbs-views/breadcrumbs [:cloud/get-app-data]]]]]
   
   [:div.pure-u-1
    ;; header panel
    ;; breadcrumbs
    ;; cloud
    [:div.pure-u-1.padded-as-button
     [general-cloud]]
    [:div.padded-as-button {:style {:position "fixed" :left "0px" :right "0px" :bottom "0px"  :background "white" :box-shadow "2px 0px 3px 0px grey"}}
     [nodes-selected-view]]]])
