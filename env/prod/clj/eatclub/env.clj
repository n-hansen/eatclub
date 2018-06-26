(ns eatclub.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[eatclub started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[eatclub has shut down successfully]=-"))
   :middleware identity})
