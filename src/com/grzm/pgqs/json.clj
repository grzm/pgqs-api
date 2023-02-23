(ns com.grzm.pgqs.json
  (:require
   [clojure.data.json :as json]))

(defn write-str [x]
  (json/write-str x))

(defn read-str [s]
  (json/read-str s :key-fn keyword))
