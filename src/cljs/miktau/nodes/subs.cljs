(ns miktau.nodes.subs
  (:require [re-frame.core :as refe]
            [clojure.string :as cljs-string]
            [miktau.tools :as utils]
            [miktau.meta-db :refer [meta-page?]]))

(defn selection-mode? [db _]
  (if-not (meta-page? db :nodes)
    false
    (not (empty? (:nodes-selected db)))))
(refe/reg-sub :nodes/selection-mode? selection-mode?)

(refe/reg-sub :nodes/get-db-for-test-purposes (fn [db _] db))
(comment
  (println   (:cloud-can-select (:breadcrumbs @(refe/subscribe [:nodes/get-db-for-test-purposes])))))

(defn order-by
  [db _]
  (let [nodes-sorted (:nodes-sorted db)]
    {:name
     {:name "Name"
      :items
      {:less {:name "a-z" :on-click [:nodes/sort "name"]  :enabled? (= nodes-sorted "name") }
       :more {:name "z-a" :on-click [:nodes/sort "-name"] :enabled? (= nodes-sorted "-name")}}}
     :modified
     {:name "Modified"
      :items {:less {:name "recent" :on-click [:nodes/sort "modified"]  :enabled? (= nodes-sorted "-modified")}
              :more {:name "older"  :on-click [:nodes/sort "-modified"]   :enabled? (= nodes-sorted "modified")}}}}))

(refe/reg-sub :nodes/order-by order-by)

(defn breadcrumbs [db _]
  (if-not (meta-page? db :nodes)
    {}
    (let [calendar-crumb (fn [field]
                           (if-let [item (field (:calendar-selected db))]
                             {:name (str (name field) ": " (utils/pad item 2 0))
                              :on-click [:nodes/click-on-calendar-item field  item]} nil))
          ranker (:cloud (:breadcrumbs db))
          selectable-items 
          (if (and (empty? (:cloud-selected db)) (empty? (:calendar-selected db)))
            (keys (:children (:tree-tag (:breadcrumbs db))))
            (keys (:cloud-can-select  (:breadcrumbs db))))]
      {:calendar
       (filter
        (comp not nil?)
        [(calendar-crumb :year)
         (calendar-crumb :month)
         (calendar-crumb :day)])
       :show-all? (:show-all? (:breadcrumbs db))
       :can-expand? (> (count selectable-items) 8)
       :cloud-items
       (let [click-children (:cloud-selected  db)]
         (for [[index item]  (map list  (range) (:cloud-selected  db))]
           {:name (str (name item))  :on-click [:nodes/clicked-many-cloud-items (take (inc index) click-children)]}))
       :cloud-can-select
       (sort-by
        :rank
        (filter
         (comp not empty?)
         (for [item  selectable-items]
           (if (contains? (:cloud-selected db) item)
             {}
             {:name (str (name item)) :rank (- (item ranker)) :on-click [:nodes/clicked-cloud-item   item]}))))})))
(refe/reg-sub :nodes/breadcrumbs breadcrumbs)

(defn pagination
  [db _]
  (let [first? (atom true)]
    {:size (:page-size db)
     :total (:total-nodes db)
     :pages
     (for [page (utils/paginate (:page db ) (:total-pages db))]
       {:name (:name page)
        :page (:page page)
        :fedots? (if (and (= "..." (:name page)) @first?)
                   (do (reset! first? false) true) false)
        :current? (:cur? page)
        :active? (not= (:name page) "...")
        :on-click
        (if-not (= (:name page) "...")
          [:nodes/to-page (:page page)]
          [:identity])})}))

(refe/reg-sub :nodes/pagination pagination)
(defn single-node-item
  [cloud-selected nodes-selected all-selected? node-prev node-next]
  (let [selected?
        (if all-selected? true (contains? nodes-selected (:id node-next)))
        prev-tags (utils/pad-coll (count (:tags node-next)) (:tags node-prev) nil) 
        all-tags
        (for [[tag-prev tag-next]  (map list  prev-tags (node-next :tags))]
          (let [same? (= tag-prev tag-next)
                tag-name (str (name tag-next))]
            {:name           tag-name
             :same-as-prev?  same?
             :key-name       (keyword (str tag-next))
             :selected?      (contains? cloud-selected  (keyword (str tag-next)))
             :can-select?    true}))]
    {:selected? selected?
     :modified (node-next :modified)
     :modified-as-prev?
     (= (:modified node-next) (:modified node-prev))
     :id (:id node-next)
     :on-click [:nodes/select-node (:id node-next)]
     :name (:name node-next)
     :all-tags-repeat-as-prev? (:same-as-prev? (last all-tags))
     :all-tags (map keyword (node-next :tags))
     :tags all-tags}))

(defn amount-selected
  [db _]
  (if-not (meta-page? db :nodes)
    {:amount 0 :total 0 :is-selected? false}
    (let [all-selected?  (contains? (:nodes-selected db) "*")
          amount (if all-selected?
                   (:total-nodes db)
                   (count (:nodes-selected db)))]
      
      {:total (:total-nodes db)
       :is-selected?  (not= amount 0)
       :amount amount})))
(refe/reg-sub :nodes/amount-selected amount-selected)

(defn node-items
  "TESTED"
  [db _]
  (if-not (meta-page? db :nodes)
    []
    (try
      (let [all-selected?  (contains? (:nodes-selected db) "*")]
        {:ordered-by
         (utils/parse-sorting-field (:nodes-sorted db))
         :total-nodes (:total-nodes db)
         :all-selected? all-selected?
         :nodes
         (cons
          (single-node-item (:cloud-selected db) (:nodes-selected db) all-selected? nil (first (:nodes db)))
          (for [[node-prev node-next] (partition 2 1 (:nodes db))]
            (single-node-item (:cloud-selected db) (:nodes-selected db) all-selected? node-prev node-next)))})
      (catch :default e {}))))
(refe/reg-sub :nodes/node-items node-items)

(defn nodes-selection-view
  [db _]
  (if-not (meta-page? db :nodes)
    {}
    (let [amount (:amount (amount-selected db _))]
      {:total          amount
       :narrow-results {:name "Edit tags"
                        :on-click [:nodes/redirect-to-edit-nodes]
                        :disabled? false}
       :links
       [{:name "Open in a single folder"
         :on-click  [:nodes/file-op :symlinks]
         :disabled? (> amount 150)}
        {:name "Open each file individually"
         :on-click  [:nodes/file-op :filebrowser]
         :disabled? (> amount 20)}
        {:name "Open each in default program"
         :on-click  [:nodes/file-op :default]
         :disabled? (> amount 10)}]})))
(refe/reg-sub :nodes/nodes-selection nodes-selection-view)



