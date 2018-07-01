(ns eatclub.db.core
  (:require
    [clj-time.jdbc]
    [conman.core :as conman]
    [java-time :as time]
    [mount.core :refer [defstate]]
    [eatclub.config :refer [env]]))

(defstate ^:dynamic *db*
          :start (conman/connect! {:jdbc-url (env :database-url)})
          :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn result->id
  "get the row id from an insert result"
  [result]
  ;; h2 specific
  (get result (keyword "scope_identity()")))

(defn h2-tsz
  "H2 hands back their own class for timestamps with timezone, so we need to translate it"
  [t]
  (time/plus (time/offset-date-time (.getYear t)
                                    (.getMonth t)
                                    (.getDay t)
                                    0 0 0 0
                                    (/ (.getTimeZoneOffsetMins t) 60))
             (time/nanos (.getNanosSinceMidnight t))))

(defn get-snapshots
  [args]
  (map #(update % :timestamp h2-tsz)
       (get-snapshots* args)))

