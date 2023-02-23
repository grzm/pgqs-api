(ns com.grzm.pgqs.client.api.impl
  (:require
   [com.grzm.pgqs.json :as json]
   [next.jdbc :as jdbc])
  (:import
   (org.postgresql.util PGobject)))

(defn jsonb-pg-object
  [x]
  (let [pg-type (or (:pg-type (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pg-type)
      (.setValue (json/write-str x)))))

(defn parse-pg-object [^PGobject v]
  (let [typ (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} typ)
      (when value
        (with-meta (json/read-str value) {:pg-type typ}))
      value)))

(defn invoke-sql-vec [op request]
  ["SELECT pgqs.invoke(?, ?)" (name op) (jsonb-pg-object request)])

(defmulti build-sql-vec (fn [op-map] (:op op-map)))

(defmethod build-sql-vec :default
  [{:keys [op request]}]
  (invoke-sql-vec op request))

(defmulti handle-response (fn [op-map _response] (:op op-map)))

(defmethod handle-response :default
  [_op-map response]
  (some-> response
          first
          :invoke
          parse-pg-object))

(defn invoke [{:keys [data-source]} op-map]
  (let [sql-vec (build-sql-vec op-map)]
    (->> (jdbc/execute! data-source sql-vec)
         (handle-response op-map))))
