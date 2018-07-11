(ns eatclub.routes
  (:require [cheshire.core :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [eatclub.graphql.core :as graphql]
            [ring.util.http-response :refer :all]
            [ring.util.request :as request]))

(defroutes app-routes
  (context "/api/graphql" []
    (GET "/" [query variables]
      (ok (graphql/execute-query query (json/parse-string variables))))
    (POST "/" [query variables :as {body :body :as r}]
      (cond
        query (ok (graphql/execute-query query (json/parse-string variables)))

        (= (request/content-type r) "application/json")
        (let [{:keys [query variables]} (json/parse-string (slurp body))]
          (ok (graphql/execute-query query variables)))

        (= (request/content-type r) "application/graphql")
        (ok (graphql/execute-query (slurp body) (json/parse-string variables)))

        :else (bad-request "I didn't understand your graphql request."))))
  (route/not-found "page not found"))
