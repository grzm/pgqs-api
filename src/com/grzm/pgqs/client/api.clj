(ns com.grzm.pgqs.client.api
  (:require
   [com.grzm.pgqs.client.api.impl :as impl]))

(defn client [{:keys [data-source]}]
  {:data-source data-source})

(defn invoke
  [client op-map]
  (try
    (impl/invoke client op-map)
    (catch Throwable t
      {:cognitect.anomalies/category :cognitect.anomalies/fault
       :cognitect.anomalies/message (.getMessage t)
       ::t t})))
