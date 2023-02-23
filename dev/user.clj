(ns user
  (:require
   [clojure.core.async :as async]
   [com.grzm.pgqs.client.api :as pgqs]
   [hikari-cp.core :as hikari-cp]))

(defn anomaly? [x]
  (when (and (map? x) (:cognitect.anomalies/category x))
    x))

(defn print-anomaly [anom]
  (binding [*out* *err*]
    (println (:cognitect.anomalies/message anom))))

(defn receive-messages [pgqs queue-url n]
  (let [res (-> (pgqs/invoke pgqs {:op :ReceiveMessage
                                   :request {:QueueUrl queue-url
                                             :MaxNumberOfMessages n}}))]
    (if (anomaly? res)
      (print-anomaly res)
      (:Messages res))))

(defn delete-message [pgqs queue-url receipt-handle]
  (let [res (pgqs/invoke pgqs {:op :DeleteMessage
                               :request {:QueueUrl queue-url
                                         :ReceiptHandle receipt-handle}})]
    (when (anomaly? res)
      (print-anomaly res))))

(defn delete-message-batch [pgqs queue-url receipt-handles]
  (let [entries (->> receipt-handles
                     (map-indexed (fn [idx receipt-handle]
                                    {:Id idx, :ReceiptHandle receipt-handle})))
        op-map {:op :DeleteMessageBatch
                :request {:QueueUrl queue-url
                          :Entries entries}}
        res (-> (pgqs/invoke pgqs op-map))]
    (when (anomaly? res)
      (print-anomaly res))))

(comment

  (def db-spec {:dbtype "postgresql"
                :username "grzm"
                :port 5499
                :dbname "dev"})
  (require '[next.jdbc :as jdbc])

  (def data-source (jdbc/get-datasource db-spec))

  (def pgqs (pgqs/client {:data-source data-source}))

  @(def my-queue (pgqs/invoke pgqs {:op :CreateQueue,
                                    :request {:QueueName "my-queue"}}))
  ;; => {:QueueUrl
  ;;     "pgqs://pgqs.system:0a10f4e7-b7e1-433f-8841-32c0f1717898/my-queue"}


  (def queue-url (:QueueUrl my-queue))
  ;; => #'user/queue-url

  (pgqs/invoke pgqs {:op :SendMessage,
                     :request {:QueueUrl queue-url
                               :MessageBody "Hello, world!"}})
  ;; => {:MessageId "c309d401-1c82-460e-a451-36d6c8dc4550",
  ;;     :MD5OfMessageBody "6cd3556deb0da54bca060b4c39479839",
  ;;     :MD5OfMessageAttributes "d41d8cd98f00b204e9800998ecf8427e",
  ;;     :MD5OfMessageSystemAttributes "d41d8cd98f00b204e9800998ecf8427e"}

  @(def received (pgqs/invoke pgqs {:op :ReceiveMessage,
                                    :request {:QueueUrl queue-url
                                              :MaxNumberOfMessages 5}}))
  ;; => {:Messages
  ;;     [{:Body "Hello, world!",
  ;;       :MD5OfBody "6cd3556deb0da54bca060b4c39479839",
  ;;       :MessageId "c309d401-1c82-460e-a451-36d6c8dc4550",
  ;;       :ReceiptHandle
  ;;       "cGdxczowYTEwZjRlNy1iN2UxLTQzM2YtODg0MS0zMmMwZjE3MTc4OTg6bXktcXVldWU6YzMwOWQ0\nMDEtMWM4Mi00NjBlLWE0NTEtMzZkNmM4ZGM0NTUwOjlmNGMyOWFhLTAzMzYtNGZjMC1iMmNmLWUz\nZTAyMTA5NWViMw=="}]}


  (pgqs/invoke pgqs {:op :DeleteMessage
                     :request {:QueueUrl queue-url
                               :ReceiptHandle (-> received :Messages first :ReceiptHandle)}})
  ;; => nil



  (do
    (def datasource-opts {:pool-name "db-pool"
                          :adapter "postgresql"
                          :username "grzm"
                          :maximum-pool-size 6
                          :database-name "dev"
                          :port-number 5499})

    (def ds (hikari-cp/make-datasource datasource-opts))

    (def queue-name "my-queue")

    :done)

  (def pgqs (pgqs/client {:data-source ds}))

  (do
    @(def create-res (pgqs/invoke pgqs {:op :CreateQueue,
                                        :request {:QueueName queue-name}}))
    (def queue_url (:QueueUrl create-res))
    :done)

  (do
    @(def get-queue-url-res (pgqs/invoke pgqs {:op :GetQueueUrl,
                                               :request {:QueueName queue-name}}))
    @(def queue-url (:QueueUrl get-queue-url-res))
    :done)


  (dotimes [_ 12]
    (async/thread
      ((fn []
         (dotimes [_ 10000]
           (let [res (pgqs/invoke pgqs {:op :SendMessage
                                        :request {:QueueUrl queue-url
                                                  :MessageBody (str "some message body " (random-uuid))}})]
             (when (anomaly? res)
               (print-anomaly res))))))))


  (dotimes [_ 10]
    (async/thread
      (let [n 10]
        (loop [received-messages (receive-messages pgqs queue-url n)]
          (when (seq received-messages)
            (doseq [{:keys [ReceiptHandle]} received-messages]
              (delete-message pgqs queue-url ReceiptHandle))
            (recur (receive-messages pgqs queue-url n)))))))

  ;; batches

  (dotimes [_ 12]
    (async/thread
      ((fn []
         (dotimes [_ 1000]
           (let [entries (->> (repeat 10 {:MessageBody (str "some message body" (random-uuid))})
                              (map-indexed (fn [idx msg]
                                             (assoc msg :Id idx))))
                 op-map {:op :SendMessageBatch
                         :request {:QueueUrl queue-url
                                   :Entries entries}}
                 res (pgqs/invoke pgqs op-map)]
             (when (anomaly? res)
               (print-anomaly res))))))))


  (dotimes [_ 10]
    (async/thread
      (let [n 10]
        (loop [received-messages (receive-messages pgqs queue-url n)]
          (when (seq received-messages)
            (let [receipt-handles (map :ReceiptHandle received-messages)]
              (delete-message-batch pgqs queue-url receipt-handles))
            (recur (receive-messages pgqs queue-url n)))))))


  :end)
