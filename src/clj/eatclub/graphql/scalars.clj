(ns eatclub.graphql.scalars
  (:require [com.walmartlabs.lacinia.schema :as schema]
            [java-time :as time])
  (:import [java.time.format DateTimeFormatter]))

(def ^:private transformers (atom {}))

(defmacro deftransformer
  [ident & fn-body]
  `(swap! transformers
          assoc
          ~ident
          (schema/as-conformer (fn ~@fn-body))))

;;;; Date

(def date-formatter DateTimeFormatter/BASIC_ISO_DATE)

(deftransformer :date/parse
  [s]
  (time/local-date date-formatter s))

(deftransformer :date/serialize
  [t]
  (time/format date-formatter t))

;;;; Timestamp

(def timestamp-formatter DateTimeFormatter/ISO_DATE_TIME)

(deftransformer :timestamp/parse
  [s]
  (time/offset-date-time timestamp-formatter s))

(deftransformer :timestamp/serialize
  [t]
  (time/format timestamp-formatter t))


;;;; Export map

(def transformer-map @transformers)


(comment
  (clojure.spec.alpha/conform (:date/serialize transformer-map) (time/offset-date-time))

  (clojure.spec.alpha/conform (:date/parse transformer-map) "2018-06-30T22:47:02.639-07:00")
  )
