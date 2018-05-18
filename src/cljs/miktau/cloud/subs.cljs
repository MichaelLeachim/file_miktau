(ns miktau.cloud.subs
  (:require [re-frame.core :as refe]
            [clojure.string :as cljs-string]
            [miktau.meta-db :refer [meta-page?]]
            [miktau.utils :as utils]))
(defn filtering [db _]
  (or (:filtering db) ""))
(refe/reg-sub :cloud/filtering filtering)

(defn cloud-filtering-should-display?
  [db]
  (if-not (meta-page? db :cloud)
    (fn [_] true)
    (if (empty? (:filtering  db))
      (fn [_] true)
      (let [compara (cljs-string/lower-case (str (:filtering db)))]
        (fn [item]
          (cljs-string/includes? (str (:compare-name item)) compara))))))

(defn get-db-for-test-purposes [db _]
  ;; (if-not (meta-page? db :cloud)
    ;; {}
  db)
;; )
(refe/reg-sub :cloud/get-db-for-test-purposes get-db-for-test-purposes)

(comment
  (println (:meta @(refe/subscribe [:cloud/get-db-for-test-purposes])))
  (refe/dispatch [:cloud/get-app-data])
  )

;; the algorithm
;; if tag is selected, then the tree must show its children

(defn general-tree
  "TESTED"
  [item pad-level cloud-can-select cloud-selected]
  (if (empty? (:name item))
    []
    (let [keyworded   (keyword (:name item))
          can-select? (contains? cloud-can-select  keyworded)
          selected?   (contains? cloud-selected  keyworded)
          leaf?       (empty? (:children item))
          base   (str (:name item))]
      (flatten
       [{:name    base
         :compare-name (cljs-string/lower-case base)
         :key-name keyworded
         :size     1
         :weighted-size  1
         :header?        (and  selected? (not leaf?))
         :leaf?          leaf?
         :disabled?      (and (not can-select?) (not selected?))
         :selected?      selected?
         :pad-level      pad-level
         :pad-background-class (str "rise-to-shadow-" pad-level)
         :can-select?    can-select?}
        (cond
          (and (= keyworded :root) (zero? pad-level))
          (for [child (vals (:children item))]
            (general-tree child (inc pad-level) cloud-can-select cloud-selected))
          (empty? cloud-selected)
          (if (<  pad-level 1)
            (for [child (vals (:children item))]
              (general-tree child (inc pad-level) cloud-can-select cloud-selected))
            [])
          selected?
          (for [child (vals (:children item))]
            (general-tree child (inc pad-level) cloud-can-select cloud-selected))
          :else
          [])]))))
(defn general-tree-subscription
  [db _]
  (if-not (meta-page? db :cloud)
     []
     (rest (general-tree (:tree-tag db) 0 (into #{} (keys (:cloud-can-select db))) (:cloud-selected db)))))

(refe/reg-sub :cloud/general-tree general-tree-subscription)

(defn cloud
  "TESTED"
  [db _]
  (if-not (meta-page? db :cloud)
    []
    (try
      (for [[group-name group] (:cloud db)]
        (let [max-size (apply max (vals group))
              filterer (cloud-filtering-should-display? db) 
              should-display?
              (fn [group]
                (filter filterer group))]
          {:group-name (str (name group-name))
           :max-size   max-size
           :group
           (sort-by
            :compare-name
            (should-display?
             (for [[tag tag-size] group]
               {:name    (str (name tag))
                :compare-name (cljs-string/lower-case (str (name tag)))
                :key-name tag
                :size     tag-size
                :group    group-name
                :weighted-size (/ tag-size max-size)
                :disabled?      (not (contains? (:cloud-can-select db) tag))
                :selected?      (contains? (db :cloud-selected) tag)
                :can-select?    (contains? (:cloud-can-select db) tag)})))}))
      (catch :default e []))))

(refe/reg-sub :cloud/cloud cloud)

(defn calendar
  "TESTED"
  [db _]
  (if-not (meta-page? db :cloud)
    {}
    (try
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
                 (let [parsed-name (utils/mik-parse-int  (str (name tag)) 0)
                       can-select? (contains? (get (:calendar-can-select db) group-name) tag)]
                   {:name   (utils/pad (str (name tag)) 2 "0") 
                    :key-name tag
                    :sort-name parsed-name
                    :size     tag-size
                    :group   group-name
                    :weighted-size (/ tag-size max-size)
                    :disabled?      (not can-select?)
                    :selected?      (= (get (:calendar-selected db) group-name) parsed-name)
                    :can-select?    can-select?}))))})]))
      (catch :default e
        (println e)
        {}))))
(refe/reg-sub :cloud/calendar calendar)

(defn fast-access-calendar
  "TESTED"
  [db _]
  (if-not (meta-page? db :cloud)
    []
    (try
      (if (:date-now db)
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
          :selected? false}]
        [])
      (catch :default e []))))

(refe/reg-sub :cloud/fast-access-calendar fast-access-calendar)



(defn selection-cloud
  "TESTED"
  [db _]
  (if-not (meta-page? db :cloud)
    []
    (cond
      (empty? (:cloud-can-select db)) []
      (empty? (:cloud-selected  db))  []
      :else
      (sort-by
       :compare-name
       (let [filterer (cloud-filtering-should-display? db) 
             should-display?
             (fn [group]
               (filter filterer group))]
         (should-display?
          (for [[tag _] (:cloud-can-select db )]
            {:name    (str (name tag))
             :compare-name (cljs-string/lower-case (str (name tag)))
             :key-name tag
             :weighted-size  1
             :size           1
             :selected?      (contains? (db :cloud-selected) tag)
             :can-select?    true})))))))

(refe/reg-sub :cloud/selection-cloud selection-cloud)