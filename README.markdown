# pgqs-api -- Clojure driver for pgqs PostgreSQL Queue Service

[pgqs](https://github.com/grzm/pgqs) is a PostgreSQL-backed queue
service with a AWS SQS-like API.

pgqs-api is a Clojure client for pgqs.

alpha. (Is there a letter before alpha?)

## Usage

```clojure
(require '[com.grzm.pgqs.client.api :as pgqs])
(require '[next.jdbc :as jdbc])

(def db-spec {:dbtype "postgresql"
              :username "grzm"
              :dbname "pgqs"})

(def data-source (jdbc/get-datasource db-spec))

(def pgqs (pgqs/client {:data-source data-source}))

@(def my-queue (pgqs/invoke pgqs {:op :CreateQueue,
                                 :request {:QueueName "my-queue"}}))

;; => {:QueueUrl
;;     "pgqs://pgqs.system:0a10f4e7-b7e1-433f-8841-32c0f1717898/my-queue"}

(def queue-url (:QueueUrl my-queue))

(pgqs/invoke pgqs {:op :SendMessage,
                   :request {:QueueUrl queue-url
                             :MessageBody "Hello, world!"}})
  ;; => {:MessageId "c309d401-1c82-460e-a451-36d6c8dc4550",
  ;;     :MD5OfMessageBody "6cd3556deb0da54bca060b4c39479839"}

@(def received (pgqs/invoke pgqs {:op :ReceiveMessage,
                                  :request {:QueueUrl queue-url
                                            :MaxNumberOfMessages 5}}))
  ;; => {:Messages
  ;;     [{:Body "Hello, world!",
  ;;       :MD5OfBody "6cd3556deb0da54bca060b4c39479839",
  ;;       :MessageId "c309d401-1c82-460e-a451-36d6c8dc4550",
  ;;       :ReceiptHandle
  ;;       "cGdxczowYTEwZjRlNy1iN2UxLTQzM2YtODg0MS0zMmMwZjE3MTc4OTg6bXktcXVldWU6YzMwOWQ0\nMDEtMWM4Mi00NjBlLWE0NTEtMzZkNmM4ZGM0NTUwOjlmNGMyOWFhLTAzMzYtNGZjMC1iMmNmLWUz\nZTAyMTA5NWViMw=="}]}

(def receipt-handle (-> received :Messages first :ReceiptHandle)

(pgqs/invoke pgqs {:op :DeleteMessage
                   :request {:QueueUrl queue-url
                             :ReceiptHandle receipt-handle}})
```


## TODO
* Add babashka-compatible client
* Add specs for requests
* Add examples of maintenance operations

## License
Copyright © 2023, Michael Glaesemann

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License.  You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.
