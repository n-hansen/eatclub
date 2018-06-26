(ns eatclub.ec.client
  (:require [cheshire.core :as json]
            [clj-http.client :as http-client]
            [clj-http.cookies :as cookies]
            [clojure.set :as set]
            [clojure.string :as string]))

(def ^:const user-agent "Mozilla/4.0 WebTV/2.6 (compatible; MSIE 4.0)")

(def headers {"user-agent" user-agent})

(def endpoints
  {:login "https://www.eatclub.com/public/api/log-in/"
   :menu-dates "https://www.eatclub.com/api/v3/menu-dates/"
   :user "https://www.eatclub.com/member/api/user/"
   :future-orders "https://www.eatclub.com/member/api/future-orders/"
   :menu "https://www.eatclub.com/menus/"})

(def cookie-store (cookies/cookie-store))

(defn- json->edn
  [str]
  (json/parse-string str #(-> %
                              (string/replace #"_" "-")
                              (string/lower-case)
                              keyword)))

(defn login
  [email password]
  (-> (http-client/put (:login endpoints)
                       {:headers headers
                        :cookie-store cookie-store
                        :content-type :json
                        :accept :json
                        :body (json/generate-string {:email email
                                                     :password password})})
      :body
      json->edn))

(comment
  (login email password)
  (cookies/get-cookies cookie-store)
  )

(defn get-open-dates
  []
  (let [location-id (-> (http-client/get (:user endpoints)
                                         {:headers headers
                                          :cookie-store cookie-store
                                          :accept :json})
                        :body
                        json->edn
                        :selected-location
                        :id)
        {:keys [menu-dates closed-dates holidays holiday-details]}
        (-> (http-client/get (:menu-dates endpoints)
                             {:headers headers
                              :accept :json
                              :content-type :json
                              :cookie-store cookie-store
                              :query-params {"include_today" true
                                             "location_id" location-id}})
            :body
            json->edn)]
    (->> menu-dates
         (map-indexed #(do {:ix (inc %1) :date %2})) ; YES IT REALLY IS 1-INDEXED
         (remove (comp (set/union (set closed-dates) (set holidays)) :date)))))

(comment
  (get-open-dates)
  )

(comment
  ; check what's already been ordered
  (-> (http-client/get (:future-orders endpoints)
                       {:headers headers
                        :cookie-store cookie-store
                        :content-type :json
                        :accept :json
                        :query-params {"status" "active"}})
      :body
      json->edn))

(defn get-menu
  [day-ix]
  (-> (http-client/get (:menu endpoints)
                       {:accept :json
                        :content-type :json
                        :headers headers
                        :cookie-store cookie-store
                        :query-params {"categorized_menu" true
                                       "day" day-ix
                                       "menu_type" "individual"}})
      :body
      json->edn))

(comment
  (-> (get-open-dates)
      first
      :date
      get-menu)
  )

(defn parse-menu
  [{:keys [date items]}]
  (for [{:keys [nutrition-estimate restaurant inventory food-category]
         item-name :item :as item} (vals items)
        :let [{restaurant-name :name} restaurant
              {category :name} food-category
              {:keys [hidden remaining]} inventory
              {:strs [calories fat carbs protein]} (->> nutrition-estimate
                                                        :estimates
                                                        (filter :value)
                                                        (map (fn [{:keys [name value]}]
                                                               [(string/lower-case name) value]))
                                                        (into {}))]]
    (merge (select-keys item [:id :is-new :average-rating :review-count :price])
           {:item-name item-name
            :restaurant-name restaurant-name
            :category category
            :hidden hidden
            :remaining remaining
            :calories calories
            :fat fat
            :carbs carbs
            :protein protein})))

(comment
  (-> (get-open-dates)
      first
      :ix
      get-menu
      parse-menu)
  )
