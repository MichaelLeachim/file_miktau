(ns miktau.views.core
  (:require [miktau.views.utils :as views-utils]
            [re-frame.core :as refe]
            [clojure.string :as clojure-string]
            [miktau.utils :as utils]
            [miktau.lorem :as lorem]))

(defn log-item [item]
  (do
    (.log js/console (str item))
    item))

(defn e->content
  [e]
  (str
   (aget e "target" "value" )))

(defn selection-cloud []
  [:div 
   (for [tag  @(refe/subscribe [:selection-cloud])]
     [:a.tag.padded-as-button
      {:key (:key-name tag)
       :href "#"
       :on-click #(refe/dispatch [:clicked-cloud-item (:key-name tag)])
       :class
       (str
        (cond (:selected? tag) "selected"
              (:can-select? tag) "can-select"
              :else "disabled"))
       :style
       {:font-weight "300"
        :text-decoration "none"}}
      (:name tag) " "])])
(defn general-cloud-tag-item [tag]
  [:a.tag
   {:key  (:key-name tag)
    :href "#"
    :on-click
    (if (:disabled? tag)
      #(refe/dispatch [:clicked-disabled-cloud-item (tag :key-name)])
      #(refe/dispatch [:clicked-cloud-item (tag :key-name)]))
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
   (for [tag  (:group (first @(refe/subscribe [:cloud])))]
     [:span {:key (:key-name tag)}
      [general-cloud-tag-item tag]])])


(defn general-tree []
  [:div
   (for [tag @(refe/subscribe [:general-tree])]
     [:a.tag
      {:key  (str  (:key-name tag) (:pad-level tag))
       :href "#"
       :on-click
       (if (:disabled? tag)
         #(refe/dispatch [:clicked-disabled-cloud-item (tag :key-name)])
         #(refe/dispatch [:clicked-cloud-item (tag :key-name)]))
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

(defn facet-group-select-time-subwidget
  [icon-name group-name additional-item-classes items]
  [:div.pure-g {:role "group"}
   [:h2.pure-u-1.mik-cut-bottom.mik-cut-top.padded-as-button.light-gray.header-font
    [views-utils/icon icon-name] group-name]
   [:div.pure-u-1
    (for [item items]
      [:a.mik-flush-center {:key   (str (item :key-name))
           :style {:cursor "pointer" :display "inline-block"}
           :on-click
           (if (:disabled? item)
             #(refe/dispatch [:clicked-disabled-calendar-item (:group item) (item :key-name)])
             #(refe/dispatch [:click-on-calendar-item (:group item) (item :key-name)]))
           :class
           (str
            (cond (:selected? item) "selected"
                  (:can-select? item) "can-select"
                  :else "disabled")
            " " additional-item-classes)}
       (:name item)])]])

(defn  facet-group-select-time []
  (let [calendar @(refe/subscribe [:calendar])]
    [:div.pure-g
     ;; fast selection
     ;; (facet-group-select-time-subwidget
     ;;  "timeline" "Filter on" ""
     ;;  @(refe/subscribe [:fast-access-calendar]))
     
     ;; year      
     [facet-group-select-time-subwidget
      "line_style"
      (:group-name (:year calendar))
      " pure-u-1-3 tag "
      (:group (:year calendar))]
     ;; month
     [facet-group-select-time-subwidget
      "date_range"
      (:group-name (:month calendar))
      " pure-u-1-5 tag "
      (:group (:month calendar))]
     ;; day
     [facet-group-select-time-subwidget
      "date_range"
      (:group-name (:day calendar))
      " pure-u-1-5 tag"
      (:group (:day calendar))]]))

(defn tagging-now-group []
  (let [nodes-changing @(refe/subscribe [:nodes-changing])]
    [:div.background-1.padded-as-button {:style {:height "100%"}}
     ;; group op
     [:div.mik-flush-right
      [views-utils/icon "photo_size_select_small"]
      " Selected " [:b (:total-amount nodes-changing)] " files"
      [:br]
      [:a.unstyled-link {:href "#" :on-click #(refe/dispatch [:cancel-tagging])} " Unselect"]]
     [:div.padded-as-button
      [:h2.mik-cut-bottom.gray "Open selected files"]
      (if (>  (:total-amount nodes-changing) 20)
        [:span {:key "if-more-than-N"}
         [:p.warning
          [:i.material-icons {:style {:font-size "1.2em" :float "right"}} "warning"]
          "You've selected " [:b ] (:total-amount nodes-changing) " files" [:br]
          "Opening them all will hang your computer" [:br]
          "Reduce amount of files to less than 20 in a selection" [:br]
          "To successfuly open them"]]
        [:span {:key "if-less-than-N"}
         [:a.mik-cut-left.unstyled-link.pure-button {:key "in a single folder" :href "#" :on-click #(refe/dispatch [:file-operation :in-folder])}
          [views-utils/icon "folder_open"] " In a single folder"]
         [:a.unstyled-link.pure-button  {:key "each individually" :href "#" :on-click #(refe/dispatch [:file-operation :individually])}
          [views-utils/icon "list"] " Each individually"]
         [:a.unstyled-link.pure-button  {:key "individually" :href "#"  :on-click #(refe/dispatch [:file-operation :default-program])}
          [views-utils/icon "filter"] " Each in default program"]])]
     
     ;; tags to remove
     [:h2.header-font.light-gray.mik-cut-bottom {:style {:font-size "1em"}}
      [views-utils/icon "local_offer"]
      "Remove tags from selection"]
     [:div 
      (for [tag (:tags-to-delete nodes-changing)]
        [:span.unstyled-link.padded-as-button
         {:key (:name tag)
          :class (if (:selected? tag) " crossed-out " "")
          :style {:cursor "pointer"  :display "inline-block"}
          :on-click #(refe/dispatch [:delete-tag-from-selection (:key-name tag)])} 
         (:name tag) " "])]
     
     ;; tags to add
     [:h2.header-font.light-gray.mik-cut-bottom {:style {:font-size "1em"}}
      [views-utils/icon "local_offer"]
      "Add tags to selection"]
     [:div
      [:textarea.padded-as-button
       {:placeholder "tag_one, tag_another, tag_third, tag_nth"
        :style  {:width "98%" :height "100px" :resize "none" }
        :on-change #(refe/dispatch [:add-tags-to-selection (e->content %)])
        :value  (:tags-to-add nodes-changing)}]]
     
     ;; changes to submit
     [:div.mik-flush-right.padded-as-button {:style {:margin-top "5em"}}
      [:a.pure-button {:href "#" :on-click #(refe/dispatch [:cancel-tagging])} [views-utils/icon "cancel"] " Cancel"]
      [:a.pure-button {:href "#" :on-click #(refe/dispatch [:submit-tagging])} [views-utils/icon "save"]   " Save"]]]))

(defn radio-button
  [text on-change selected?]
  [:label.pure-checkbox
   {:for "blab" :style {:position "relative"}}
   [:input
    {:id "blab"  :style {:width "25px" :height "25px" :cursor "pointer"} :checked selected? :type "checkbox" :on-change on-change}]
   [:span {:style {:padding-bottom "5px"}}
    text]])

(defn file-table-header [selection-mode? all-selected?]
  [:div.pure-g {:style {:padding-bottom "1em"}}
   ;; select all nodes button
   [:div.pure-u-2-24
    (views-utils/position-absolute
     {:top "0px"}
     [radio-button "" #(refe/dispatch [:select-all-nodes]) all-selected?])]
   [:div.pure-u-16-24
    (if selection-mode?
      [:div "Name"]
      [:div
       "Name"
       [:span {:style {:font-size "0.6em"}}
        "["
        [:a.unstyled-link {:href "#" :key "order-a-z"
                           :on-click #(refe/dispatch [:sort "name"])}
         "a-z"]
        "·"
        [:a.unstyled-link {:href "#" :key "order-z-a"

                           :on-click #(refe/dispatch [:sort "-name"])}
         " z-a"] "]"]])]
   [:div.pure-u-6-24.mik-flush-right
    (if selection-mode?
      [:div "Modified"]
      [:div
       [:span {:style {:font-size "0.6em"}}
        "["
        [:a.unstyled-link {:href "#" :key "order-a-z"
                           :on-click #(refe/dispatch [:sort "modified"])}
         "recent"]
        "·"
        [:a.unstyled-link {:href "#" :key "order-z-a"
                           :on-click #(refe/dispatch [:sort "-modified"])}
         " older"]
        "]"]
       "Modified"])]])

(defn tagging-in-a-single-node-item
  [tags selection-mode?]
  [:div
   (for [tag tags]
     [:a.inline-tag
      {:key (:key-name tag)
       :on-click
       (if-not selection-mode?
         #(refe/dispatch [:clicked-cloud-item (tag :key-name)])
         identity)
       :class
       (str
        (cond (:to-delete? tag) "crossed-out"
              (:to-add?    tag) "added-in"
              :else "")
        " "
        (cond
          (:selected? tag) "selected"
          (:can-select? tag) "can-select"
          :else "disabled"))
       :style {                       :pointer (if selection-mode? "default" "cursor")}}  (:name tag) " "])])

(defn single-node-item [node selection-mode?]
  [:tr
   {:key (str  (:id node))
    :data-fpath  (str (:file-path node) (:name node))
    :style {:padding-bottom "10px"
            :padding-top "10px"
            :font-size "0.8em"
            :border-bottom "solid 1px #e3e3e3"
            :cursor "pointer"}}
   [:td
    [views-utils/position-absolute
     {:top ""}
     [radio-button "" #(refe/dispatch [:select-node (str (node :file-path) (node :name))]) (:selected? node)]]]
   [:td
    [:a.unstyled-link
     {:href "#" :style {:font-weight "300" :word-wrap "break-word"}}
     (:name node)]]
   [:td
    [tagging-in-a-single-node-item (:tags node) selection-mode?]
    (cond
      selection-mode?
      [:span]
      (not  (empty? (:all-tags node)))
      [:a.unstyled-link
       {:href "#"
        :on-click
        (if-not selection-mode?
          #(refe/dispatch [:clicked-many-cloud-items (:all-tags node)])
          identity)}
       [:span
        {:style {:padding "3px" :margin "3px" :font-size "0.8em"}}
        [views-utils/icon "arrow_forward"]]]
      :else
      [:span])]
   [:td
    [:a.unstyled-link {:href "#"
                       :on-click
                       (if-not selection-mode? #(refe/dispatch  [:click-on-calendar-item "FastAccess" (node :modified)]) identity)
                       :style {:font-weight "300"}}
     (str
      (utils/pad (:year   (node :modified)) 4 "0") "."
      (utils/pad (:month  (node :modified)) 2 "0") "."
      (utils/pad (:day    (node :modified)) 2 "0"))]]])

(defn file-table []
  (let [node-items  @(refe/subscribe [:node-items])
        selection-mode? @(refe/subscribe [:selection-mode?])]
    [:div.padded-as-button.background-1
     [file-table-header selection-mode? (:all-selected? node-items)]
     [:table
      [:tbody
       (for [node (:nodes node-items)]
         ^{:key (str (:id node))}
         [single-node-item node selection-mode?])]]
     (if (> (:omitted-nodes node-items) 0)
       [:div.mik-flush-right.gray
        "Truncated: "
        [:b  (:omitted-nodes node-items)]]
       [:div])]))

(defn dropzone []
  [:div.mik-flush-center.background-1
   {:style {:padding "5em" :margin-top "1em" :margin-bottom "1em"
            :border "dashed 1px gray"}}
   [:a.pure-button {:href "#"}
    [views-utils/icon "file_upload"]
    "Drop files here"]])

(defn filter-input []
  (let [filtering (refe/subscribe [:filtering])]
    [:div.padded-as-button {:style {:position "relative"}}
     [:input.background-0
      {:type "text" :placeholder "Filter"
       :value @filtering
       :on-change #(refe/dispatch [:filtering (e->content %)])
       ;; :on-blur  #(utils/set-timeout 100 (fn [] (refe/dispatch [:filtering ""])))
       :style {:width "97%" :height "2em" :padding-left "20px" :background "white !important"}}]
     [:div {:style {:position "absolute" :right "30px" :top "23px"}}
      [:a.unstyled-link {:href "#" :on-click #(refe/dispatch [:clear])} 
       "Clear"]]
     [:div {:style {:position "absolute" :right "90px" :top "23px"}}
      [views-utils/icon "search"]]]))
(defn left-pane
  []
  [:div.pure-g
   ;; header
   [:div.pure-u-1
    [:div.pure-u-1-8
     [:div
      [:a.unstyled-link
       {:href "#"
        :on-click #(refe/dispatch [:back])
        :style
        {:font-size "3em", :padding-top "0.5em",
         :padding-left "0.5em", :padding-right "0.5em"}}
       [views-utils/icon "keyboard_arrow_left"]]]]
    [:div.pure-u-7-8
     [filter-input]]]
   ;; time facet
   [:div.pure-u-1-5.background-0
    [:div.padded-as-button
     [facet-group-select-time]]]
   
   [:div.pure-u-2-5.background-1
    [:div.padded-as-button
     [general-tree]]]
   
   ;; cloud facet
   [:div.pure-u-4-5
    ;; [:div.background-1 {:style {:overflow "hidden" :height "70px"}}
    ;;  [:div {:style {:font-size "0.7em", :padding-bottom "2em"}}
    ;;   (selection-cloud)]]
    
    [:div.background-2
     [:div 
      [general-cloud]]]]])

(defn drill
  []
  [:div.background-2

   [:div.pure-g
    [:div.background-2  {:style {:position "fixed" :width "70%" :top "0" :bottom "0" :overflow-y "scroll"}}
     (if @(refe/subscribe [:selection-mode?])
       [:div.pure-u-1 {:style {:height "100%"}}
        [tagging-now-group]]
       [left-pane])]
    ;; found, files, and tag anew group
    [:div.pure-u-1-2.background-3 {:style {:position "fixed" :width "30%" :top 0 :bottom 0 :right 0 :height "100%" :overflow-y "scroll"}}
     [:div.padded-as-button
      ;; (found-group)
      ;; [file-table]
      ]]]])

(defn choose-root
  []
  [:div
   {:style {:padding "5em", :border "dashed 1px gray"}}
   [:div.mik-flush-center.background-1
    [:a.padded-as-button.unstyled-link  {:href "#" :style {:font-size "4em"}}
     "Choose root directory"]
    [:div.mik-flush-center.background-1
     {:style
      {:padding "5em", :margin-top "1em", :margin-bottom "1em", :border "dashed 1px gray"}}
     [:a {:href "#"}
      [views-utils/icon "file_upload"]
      "Or Drop it here"]]]])

(defn processing
  []
  [:div.mik-flush-center.background-1
   {:style {:padding "5em" :border "dashed 1px gray"}}
   [:div
    [:a {:href "#" :style {:font-size "4em"}}
     "Processing..."]
    [:div.mik-flush-center.background-1
     [:img {:src "/loading.gif"}]]]])

(defn main []
  [drill])
