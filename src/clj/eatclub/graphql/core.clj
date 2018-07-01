(ns eatclub.graphql.core
  (:require [com.walmartlabs.lacinia.util :as gql-util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [eatclub.graphql.scalars :as scalars]
            [eatclub.graphql.resolvers :as resolvers]
            [java-time :as time]
            [ring.util.http-response :refer :all]
            [mount.core :refer [defstate]]))


(defstate compiled-schema
  :start
  (-> "graphql/schema.edn"
      io/resource
      slurp
      edn/read-string
      (gql-util/attach-scalar-transformers scalars/transformer-map)
      (gql-util/attach-resolvers resolvers/resolver-map)
      schema/compile))

(defn format-params [query]
   (let [parsed (json/read-str query)] ;;-> placeholder - need to ensure query meets graphql syntax
     (str "query { hero(id: \"1000\") { name appears_in }}")))

(defn execute-request [query]
    (let [vars nil
          context nil]
    (-> (lacinia/execute compiled-schema query vars context)
        (json/write-str))))
