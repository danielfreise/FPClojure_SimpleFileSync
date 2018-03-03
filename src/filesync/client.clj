(ns filesync.client
  (:gen-class)
  (:require [clojure.java.io        :as io])
  (:require [filesync.sockets       :as FPSocket])
  (:require [filesync.filefilters   :as FSFilters])
  (:import  [java.net Socket]))

(defn write
  ([client data]
   (write client data (alength data)))

  ([client data length]
   (let [streamOut (client FPSocket/KEYWORD_STREAM_OUT)]
     (try
       (FPSocket/writeToStream streamOut data length)
       (catch Exception e
         (println "Caught an exception while trying to write data to a client's stream:\n")
         (.printStackTrace e))))
   client))


(defn start
  [host port readHandler]
  (println "[MainThread] Started the application as a client instance.")
  (let [clientSocket (Socket. host port)]
    (-> clientSocket
      (FPSocket/createSocketInfo)
      (FPSocket/printSocketInfo)
      (FPSocket/startReadingAsync readHandler))))

(defn stopClient
  [client]
  (FSFilters/store-last-sync)
  (FPSocket/close client)
  client)
