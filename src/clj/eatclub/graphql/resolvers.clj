(ns eatclub.graphql.resolvers
  (:require [clojure.edn :as edn]
            [eatclub.db.core :as db]
            [java-time :as time]))

(def ^:private resolvers (atom {}))

(defmacro defresolver
  [ident & fn-body]
  `(swap! resolvers assoc ~ident (fn ~@fn-body)))

(defn- serialize-id
  [prefix id]
  (str prefix ":" id))

(defn- deserialize-id
  [prefix str]
  (when-let [[_ prefix* id] (re-matches #"(.*):([0-9]+)" str)]
    (when (= prefix prefix*)
      (edn/read-string id))))

;;;; Field Resolvers

(defresolver :item/id
  [_ _ {:keys [id]}]
  (serialize-id "item" id))

(defresolver :item/name
  [_ _ {:keys [id]}]
  (:name (db/get-item {:id id})))

(defresolver :menu/listed-items
  [_ {:keys [ids]} {:keys [date]}]
  (let [id-filter (if (not-empty ids)
                    (->> ids
                         (map #(deserialize-id "item" %))
                         (into #{}))
                    (constantly true))]
    (->> (db/get-menu-items {:menu_date date})
         (filter #(-> % :item_id id-filter))
         (map (fn [{:keys [item_id]}]
                {:item {:id item_id}
                 :menu-date date})))))

(defresolver :item-listing/snapshots
  [_ {:keys [precision aggregation window_start window_end]} {:keys [menu-date item]}]
  (->> (db/get-snapshots {:menu_date menu-date
                          :item (:id item)
                          :window_start (or window_start (time/offset-date-time 0))
                          :window_end (or window_end (time/offset-date-time))})
       (group-by (case precision
                   (:seconds :minutes :hours :days)
                   #(-> % :timestamp (time/truncate-to precision))
                   :weeks #(-> % :timestamp (time/as :year :week-of-week-based-year))
                   :months #(-> % :timestamp (time/as :month))
                   :years #(-> % :timestamp (time/as :year))
                   :alltime (constantly 0)))
       (sort-by first)
       (map second)
       (map (fn [[{:keys [timestamp]} :as entries]]
              (let [aggregated-entries
                    (case aggregation
                      :first (first entries)
                      :last (last entries)
                      :minimum (reduce (fn [a b]
                                         (merge-with
                                          #(if (< (compare %1 %2) 0) %1 %2)
                                          a b))
                                       entries)
                      :maximum (reduce (fn [a b]
                                         (merge-with
                                          #(if (> (compare %1 %2) 0) %1 %2)
                                          a b))
                                       entries)
                      :mean  (let [{:keys [quantity average_rating review_count]}
                                   (->> entries
                                        (map #(dissoc % :timestamp :hidden))
                                        (reduce #(merge-with +))
                                        (map (fn [[k v]]
                                               [k (double (/ v (count entries)))]))
                                        (into {}))]
                               {:hidden (->> entries
                                             (map :hidden)
                                             (group-by identity)
                                             (sort-by second #(compare %2 %1))
                                             first
                                             second)
                                :quantity (Math/floor quantity)
                                :average_rating average_rating
                                :review_count (Math/ceil review_count)}))]
                (assoc aggregated-entries :timestamp timestamp))))))

;;;; Query Resolvers

(defresolver :query/menu
  [_ {:keys [date]} _]
  {:date date})

;;;; Export map

(def resolver-map @resolvers)

(comment
  (db/get-menu-items {:menu_date "2018-07-02"})
  (db/get-snapshots {:menu_date "2018-07-02",
                     :item 9776
                     :window_start (time/offset-date-time 0)
                     :window_end (time/offset-date-time)})

  (time/day-of-week (:day-of-week (time/offset-date-time)))

  (time/truncate-to (time/offset-date-time) :days)

  (time/value (time/property (time/offset-date-time) :week-of-week-based-year))

  )
