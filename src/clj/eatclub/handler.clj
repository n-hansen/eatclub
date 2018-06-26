(ns eatclub.handler
  (:require 
            [eatclub.routes.services :refer [service-routes]]
            [compojure.core :refer [routes wrap-routes]]
            [ring.util.http-response :as response]
            [eatclub.middleware :as middleware]
            [compojure.route :as route]
            [eatclub.env :refer [defaults]]
            [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
    (routes
          #'service-routes
          (route/not-found
             "page not found"))))

