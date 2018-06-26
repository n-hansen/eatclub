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

(defn open-dates
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
  (open-dates)
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

(defn menu*
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

(defn menu
  [date]
  (when-let [{:keys [ix]} (->> (open-dates)
                               (filter #(-> % :date (= date)))
                               first)]
    (menu* ix)))

(comment
  (menu "2018-06-29")
  )
