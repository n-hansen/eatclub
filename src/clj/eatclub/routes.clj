(ns eatclub.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [eatclub.graphql.core :as graphql]
            [ring.util.http-response :refer :all]
            [ring.util.request :as request]))

(defroutes app-routes
  (context "/api/graphql" []
      (GET "/" [query]
        (ok (graphql/execute-query query)))
      (POST "/" [query variables :as {body :body :as r}]
        (cond
          query (ok (graphql/execute-query query variables))

          (= (request/content-type r) "application/json")
          (ok (graphql/execute-json-request (slurp body)))

          (= (request/content-type r) "application/graphql")
          (ok (graphql/execute-query (slurp body)))

          :else (bad-request "I didn't understand your graphql request."))))
  (route/not-found "page not found"))
