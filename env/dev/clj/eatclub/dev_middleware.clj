(ns eatclub.dev-middleware
  (:require [ring.middleware.reload :refer [wrap-reload]]
            [ring-graphql-ui.core :refer [wrap-graphiql wrap-voyager]]
            [selmer.middleware :refer [wrap-error-page]]
            [prone.middleware :refer [wrap-exceptions]]))

(defn wrap-dev [handler]
  (-> handler
      (wrap-graphiql {:path "/graphiql"
                      :endpoint "/api/graphql"})
      (wrap-voyager {:path "/voyager"
                     :endpoint "/api/graphql"})
      wrap-reload
      wrap-error-page
      (wrap-exceptions {:app-namespaces ['eatclub]})))
