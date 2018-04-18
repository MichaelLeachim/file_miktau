(ns miktau.subs
  (:require [re-frame.core :as refe]
            [clojure.string :as cljs-string]
            [miktau.utils :as utils]))
(defn filtering [db _]
  (or (db :filtering) ""))
(refe/reg-sub :filtering filtering)

(defn cloud
  "TESTED"
  [db _]
   (for [[group-name group] (:cloud db)]
     (let [max-size (apply max (vals group))
           should-display?
           (fn [group]
             (filter
              (fn [item]
                true) group))
           sorting #(reverse (sort :key-name %))]
       {:group-name (str (name group-name))
        :max-size   max-size
        :group
        (sorting
         (should-display?
          (for [[tag tag-size] group]
            {:name    (str (name tag))
             :compare-name (into #{} (cljs-string/lower-case (str (name tag))))
             :key-name tag
             :size     tag-size
             :group    group-name
             :wieghted-size (/ tag-size max-size)
             :selected?      (contains? (db :selected) tag)
             :can-select?    (contains? (:cloud-can-select db) tag)})))})))
(refe/reg-sub :cloud cloud)

(defn calendar
  "TESTED"
  [db _]
  (into
   {}
   (for [[group-name group] (:calendar db)]
     [group-name
      (let [max-size (apply max (vals group))
            sorter-applicator
            (fn [data]
              (if (= group-name :year)
                (reverse (sort-by :sort-name data))
                (sort-by :sort-name data)))]
        {:group-name (str (name group-name))
         :max-size max-size
         :group
         (map
          #(dissoc % :sort-name)
          (sorter-applicator
           (for [[tag tag-size] group]
             {:name    (if (=  group :month)
                         (utils/month-name tag)
                         (str (name tag))) 
              :key-name tag
              :sort-name (utils/mik-parse-int  (str (name tag)))
              :size     tag-size
              :group   group-name
              :weighted-size (/ tag-size max-size)
              :selected?      (= ((:calendar-selected db) group-name) tag)
              :can-select?    (contains? ((db :calendar-can-select) group-name) tag)})))})])))

(refe/reg-sub :calendar calendar)

(defn selection-cloud
  "TESTED"
  [db _]
  (if (empty? (db :cloud-can-selected))
    []
    (for [[tag _] (db :cloud-can-select)]
      {:name    (str (name tag))
       :key-name tag
       :weighted-size 1
       :selected?      (contains? (db :selected) tag)
       :can-select?    true})))
(refe/reg-sub :selection-cloud selection-cloud)
(defn fast-access-calendar
  "TESTED"
  [db _]
  [{:name "Today"
    :group "FastAccess"
    :can-select? (utils/is-it-today? db [:day :month :year])
    :key-name      (:date-now db)
    :selected? false}
   {:name "This month"
    :group "FastAccess"
    :can-select? (utils/is-it-today? db [:month :year])
    :key-name      (dissoc (:date-now db) :day)
    :selected? false}
   {:name "This year"
    :group "FastAccess"
    :can-select? (utils/is-it-today? db [:year])
    :key-name      {:year (:year (:date-now db))}
    :selected? false}])
(refe/reg-sub :fast-access-calendar fast-access-calendar)

(defn node-items
  "TESTED"
  [db _]
  (let [all-selected? (=  (first (db :nodes-selected)) "*")]
    {:ordered-by
     (utils/parse-sorting-field (:nodes-sorted db))
     :total-nodes (:total-nodes db)
     :ommitted-nodes (-  (:total-nodes db) (count (db :nodes)))
     :all-selected? all-selected?
     :nodes
     (for [i (db :nodes)]
       {:selected?
        (if all-selected?
          true
          (contains? (db :nodes-sorted)
                     (:file-path i)))
        :modified
        (i :modified)
        :name (:name i)
        :all-tags (map keyword (i :tags))
        :file-path (i :file-path)
        :tags
        (for [tag (into #{} (concat  (i :tags) (db :nodes-temp-tags-to-add)))]
          {:name    (str (name tag))
           :key-name (keyword (str tag))
           :to-add?        (contains? (db :nodes-temp-tags-to-add) tag)
           :to-delete?     (contains? (db :nodes-temp-tags-to-delete) tag)
           :selected?      (contains? (db :selected) (keyword tag))
           :can-select?    true})})}))

(refe/reg-sub :node-items node-items)

(defn nodes-changing
  "TESTED"
  [db _]
  (let [all-selected? (=  (first (db :nodes-selected)) "*")]
     {:display? (not (empty? (db :nodes-selected)))
      :all-selected? all-selected?
      :total-amount
      (if all-selected? (db :total-nodes)
          (count (db :nodes-selected)))
      :tags-to-add    (db :nodes-temp-tags-to-add)
      :tags-to-delete (db :nodes-temp-tags-to-delete)}))
(refe/reg-sub :nodes-changing nodes-changing)

