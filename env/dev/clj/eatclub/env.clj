(ns eatclub.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [eatclub.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[eatclub started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[eatclub has shut down successfully]=-"))
   :middleware wrap-dev})
