(ns eatclub.ec.client
  (:require [cheshire.core :as json]
            [clj-http.client :as http-client]
            [clj-http.cookies :as cookies]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [eatclub.config :refer [env]]
            [mount.core :as mount]
            [slingshot.slingshot :refer [try+ throw+]]))

(def ^:const user-agent "Mozilla/4.0 WebTV/2.6 (compatible; MSIE 4.0)")
(def ^:const reauthentication-wait 500)

(def headers {"user-agent" user-agent})

(def endpoints
  {:login "https://www.eatclub.com/public/api/log-in/"
   :menu-dates "https://www.eatclub.com/api/v3/menu-dates/"
   :user "https://www.eatclub.com/member/api/user/"
   :future-orders "https://www.eatclub.com/member/api/future-orders/"
   :menu "https://www.eatclub.com/menus/"})

(def cookie-store (cookies/cookie-store))

(defn json->edn
  [str]
  (json/parse-string str #(-> %
                              (string/replace #"_" "-")
                              (string/lower-case)
                              keyword)))

(defn login
  []
  (log/debug "Clearing cookies and attempting to authenticate.")
  (cookies/clear-cookies cookie-store)
  (-> (http-client/put (:login endpoints)
                       {:headers headers
                        :cookie-store cookie-store
                        :content-type :json
                        :accept :json
                        :body (json/generate-string {:email (:eatclub-email env)
                                                     :password (:eatclub-password env)})})
      :body
      json->edn))

(defn request-with-reauthentication
  ([request-map] (request-with-reauthentication request-map 1))
  ([request-map retries]
   (try+ (http-client/request request-map)
         (catch [:status 401] {:keys [body]}
           (if (> retries 0)
             (do
               (log/info (format "Authentication error. Reauthenticating and retrying (%s retries left)." retries))
               (Thread/sleep reauthentication-wait)
               (login)
               (request-with-reauthentication request-map (dec retries)))
             (log/error (format "Authentication error. No more retries and giving up. Request: %s, response: %s"
                                (pr-str request-map)
                                body)))))))

(defn get-open-dates
  []
  (when-let [location-id (some-> {:url (:user endpoints)
                                  :method :get
                                  :headers headers
                                  :cookie-store cookie-store
                                  :accept :json}
                                 request-with-reauthentication
                                 :body
                                 json->edn
                                 :selected-location
                                 :id)]
    (when-let [{:keys [menu-dates closed-dates holidays holiday-details]}
               (some-> {:url (:menu-dates endpoints)
                        :method :get
                        :headers headers
                        :accept :json
                        :content-type :json
                        :cookie-store cookie-store
                        :query-params {"include_today" true
                                       "location_id" location-id}}
                       request-with-reauthentication
                       :body
                       json->edn)]
      (->> menu-dates
           (map-indexed #(do {:ix (inc %1) :date %2})) ; YES IT REALLY IS 1-INDEXED
           (remove (comp (set/union (set closed-dates) (set holidays)) :date))))))

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

(defn parse-menu
  [{:keys [date items]}]
  (for [{:keys [nutrition-estimate restaurant inventory food-category]
         item-name :item, item-id :id, :as item} (vals items)
        :let [{restaurant-name :name} restaurant
              {category :name} food-category
              {:keys [hidden remaining]} inventory
              {:strs [calories fat carbs protein]} (->> nutrition-estimate
                                                        :estimates
                                                        (filter :value)
                                                        (map (fn [{:keys [name value]}]
                                                               [(string/lower-case name) value]))
                                                        (into {}))]]
    (merge (select-keys item [:is-new :average-rating :review-count :price])
           {:menu-date date
            :item-id item-id
            :item-name item-name
            :restaurant-name restaurant-name
            :category category
            :hidden hidden
            :remaining remaining
            :calories calories
            :fat fat
            :carbs carbs
            :protein protein})))

(mount/defstate authentication
  :start
  (let [{:strs [csrftoken sessionid]} (cookies/get-cookies cookie-store)]
    (when-not (and (:value csrftoken)
                   (:value sessionid))
        (login))))

(comment
  (login)
  (cookies/get-cookies cookie-store)

  (get-open-dates)

  ; check what's already been ordered
  (-> (http-client/get (:future-orders endpoints)
                       {:headers headers
                        :cookie-store cookie-store
                        :content-type :json
                        :accept :json
                        :query-params {"status" "active"}})
      :body
      json->edn)

  (-> (get-open-dates)
      first
      :ix
      get-menu)

  (-> (get-open-dates)
      first
      :ix
      get-menu
      parse-menu
      first)
  )
