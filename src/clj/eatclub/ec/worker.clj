(ns eatclub.ec.worker
  (:require [clojure.tools.logging :as log]
            [eatclub.ec.client :as client]
            [eatclub.db.core :as db]
            [taoensso.nippy :as nippy])
  (:import [java.util Date]))

(defn grab-menus
  []
  (doseq [{:keys [ix]} (client/get-open-dates)
          :let [menu (-> ix client/get-menu client/parse-menu)
                now (Date.)]]
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
                                    :review_count review-count})
          (log/info (format "Menu listing: %s -- %s" menu-date item-name)))))

(comment
  (grab-menus))
