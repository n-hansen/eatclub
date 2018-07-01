(ns eatclub.ec.worker
  (:require [clojure.tools.logging :as log]
            [eatclub.ec.client :as client]
            [eatclub.db.core :as db]
            [immutant.scheduling :as scheduling]
            [mount.core :as mount]
            [taoensso.nippy :as nippy])
  (:import [java.util Date]))

(def polling-schedule
  {:every [5 :minutes]})

(defn grab-menus
  []
  (doseq [{:keys [ix date]} (client/get-open-dates)
          :let [menu (-> ix client/get-menu client/parse-menu)
                now (Date.)]]
    (log/debug (format "Fetching menu for %s." date))
    (doseq  [{:keys [average-rating review-count price menu-date item-id item-name restaurant-name
                     category hidden remaining calories fat carbs protein]}
             menu
             :let [restaurant-id (or (-> {:name restaurant-name} db/get-restaurant-id :id)
                                     (-> {:name restaurant-name}
                                         db/create-restaurant!
                                         db/result->id))
                   category-id (or (-> {:name category} db/get-category-id :id)
                                   (-> {:name category}
                                       db/create-category!
                                       db/result->id))
                   snapshot-id (db/result->id
                                (db/create-menu-snapshot! {:snapshot_time now
                                                           :menu_date menu-date
                                                           :full_response (nippy/freeze menu)}))]]
          (when-not (db/get-item {:id item-id})
            (db/create-item! {:id item-id
                              :name item-name
                              :calories calories
                              :fat fat
                              :protein protein
                              :carbs carbs
                              :price price
                              :restaurant restaurant-id
                              :category category-id}))
          (db/create-item-listing! {:menu_snapshot snapshot-id
                                    :item item-id
                                    :quantity remaining
                                    :hidden hidden
                                    :average_rating average-rating
                                    :review_count review-count}))))

(mount/defstate worker
  :start
  (scheduling/schedule grab-menus (merge polling-schedule
                                         {:id ::polling}))
  :stop
  (scheduling/stop {:id ::polling}))

(comment
  (grab-menus))
