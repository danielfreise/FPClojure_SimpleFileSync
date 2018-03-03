(ns filesync.protocolHandler
  (:gen-class)
  (:require [filesync.utils               :as utils])
  (:require [filesync.client              :as FPClient])
  (:require [filesync.sockets             :as FPSocket])
  (:require [filesync.filemodification    :as FSMod])
  (:require [filesync.fileaccess          :as FSAccess])
  (:require [filesync.filefilters         :as FSFilters])
  (:require [filesync.protocol            :as protocol]))

(import '[java.util Arrays])

(def ^:const ERROR_NOT_SPECIFIED      (int 0x00))
(def ^:const ERROR_MALFORMED_HEADER   (int 0x01))
(def ^:const ERROR_CODE_WORD_UNKNOWN  (int 0x02))

(def SyncPairs (atom {}))
(def Disconnects (atom {}))
(def IncomingFile (atom {:name "" :size -1 :recieved 0}))


(defn addToPairs
  [syncKey socket]
  (swap! SyncPairs update syncKey #(conj % socket)))

(defn remove-pair
  [syncKey]
  (swap! SyncPairs dissoc syncKey))

(defn remove-disc
  [syncKey]
  (swap! Disconnects dissoc syncKey))

(defn getSyncPair
  [syncKey]
  (@SyncPairs syncKey))

(defn getRemoteAddress
  [socketInfo]
  (str (socketInfo FPSocket/KEYWORD_REMOTE_IP) ":" (socketInfo FPSocket/KEYWORD_REMOTE_PORT)))

(defn sendSyncStatus
  [clients newClient]
  (let [count (count clients)]
    (condp = count
      1 (FPClient/write newClient protocol/MSG_WAITS_FOR_SYNC)
      2 (doseq [client clients]
          (FPClient/write client protocol/MSG_READY_FOR_SYNC))
      (FPClient/write newClient protocol/MSG_TOO_MANY_CLIENTS))))

(defn get-sync-key-from-data [data]
  (->> data
    (take (+ protocol/PROTOCOL_HEADER_LENGTH protocol/SYNC_KEY_LENGTH))
    (drop protocol/PROTOCOL_HEADER_LENGTH)
    byte-array
    String.))

(defn get-header-from-data [data]
  (->> data
    (take protocol/PROTOCOL_HEADER_LENGTH)))

(defn get-other-client [socketInfo pair]
  (let [a (first pair) b (second pair)]
    (if (= a socketInfo)
      b
      a)))

(defn send-ignore-info 
  [socketInfo info key]
  (let [CW protocol/CW_0008_BYTES_IGNORE_FILE_INFO buffer (utils/to-bytes info)]
    (let [data (protocol/get-data-to-send CW key buffer)]
      (FPClient/write socketInfo data (alength data)))))

(defn send-file-info 
  [socketInfo info key]
  (let [CW protocol/CW_0011_BYTES_FILE_INFO buffer (utils/to-bytes info)]
    (let [data (protocol/get-data-to-send CW key buffer)]
      (FPClient/write socketInfo data (alength data)))))


(defn disconnect
  [socketInfo]
  (let [data protocol/MSG_COM_END]
    (FPClient/write socketInfo data (alength data)))
  (FPSocket/close socketInfo))

(defn set-client-disconnect
  [key socketInfo]
  (swap! Disconnects assoc key true))

(defn ask-disconnect
  [socketInfo]
  (let [msg protocol/MSG_ASK_DISC CW protocol/CW_0000_BYTES_ASK_DISC key (FSAccess/get-sync-key-bytes)]
    (let [data (protocol/get-data-to-send CW key msg)]
      (FPClient/write socketInfo data (alength data)))))


(defn send-ignore-file-data [socketInfo info key]
  (let [buffer (byte-array protocol/BUFFER_SIZE) size (info :size)]
    (with-open [instream (FSAccess/get-file-byte-stream (info :name))]
      (loop [read 0]
        (if (< read size)
          (recur 
            (do
              (FSAccess/read-stream instream buffer)
              (let [CW protocol/CW_0009_BYTES_IGNORE_FILE]
                (let [data (protocol/get-data-to-send CW key buffer)]
                  (FPClient/write socketInfo data (alength data))
                  (+ read (alength buffer)))))))))))

(defn send-file-data [socketInfo info key]
  (let [buffer (byte-array protocol/BUFFER_SIZE) size (info :size)]
    (with-open [instream (FSAccess/get-file-byte-stream (info :name))]
      (loop [read 0]
        (if (< read size)
          (recur 
            (do
              (FSAccess/read-stream instream buffer)
              (let [CW protocol/CW_0012_BYTES_FILE]
                (let [data (protocol/get-data-to-send CW key buffer)]
                  (FPClient/write socketInfo data (alength data))
                  (+ read (alength buffer)))))))))))

(defn startFileSync
  [socketInfo]
  (println "Starting file sync...")
  (let [data (FSMod/get-files-mod-info-bytes)]
    (FPClient/write socketInfo data (alength data))))

(defn send-ignore-file 
  [socketInfo info]
  (if (> (count info) 0)
    (let [transfer-info (FSAccess/get-transfer-info ((first info) :name)) key (FSAccess/get-sync-key-bytes)]
      (send-ignore-info socketInfo transfer-info key)
      (send-ignore-file-data socketInfo transfer-info key)
      (startFileSync socketInfo))))

(defn send-files [socketInfo info]
  (doseq [file info]
    (let [transfer-info (FSAccess/get-transfer-info (file :name)) key (FSAccess/get-sync-key-bytes)]
      (send-file-info socketInfo transfer-info key)
      (send-file-data socketInfo transfer-info key)))
  (ask-disconnect socketInfo))


(defn setIncomingFile [fileinfo]
  (swap! IncomingFile assoc :name (fileinfo :name))
  (swap! IncomingFile assoc :size (fileinfo :size))
  (swap! IncomingFile assoc :recieved 0))

(defn getIncomingFile []
  @IncomingFile)


(defn onError
  ([socketInfo]
   (onError socketInfo ERROR_NOT_SPECIFIED))

  ([socketInfo error]
   (println "[ERROR] Connection to remote " (getRemoteAddress socketInfo) " will be terminated for the following reason:\n "
    (condp = error
      ERROR_MALFORMED_HEADER  (str "The specified remote has sent invalid data: Malformed header.\n")
      ERROR_CODE_WORD_UNKNOWN (str "The specified remote has sent invalid data: Unknown code word.\n")
      (str "Communication with the specified remote has led to an unspecified error.\n")))
   (FPSocket/close socketInfo)))

(defn onDisconnect
  [socketInfo]
  (println "Remote with address " (getRemoteAddress socketInfo) " has terminated the connection.")
  (FPSocket/close socketInfo))

(defn onAllowDisconnect
  [socketInfo]
  (println "Remote with address " (getRemoteAddress socketInfo) " granted allowance to disconnect.")
  (FSFilters/store-last-sync)
  (FPSocket/close socketInfo))

(defn onKeepAlive
  [socketInfo]
  (println "Remote with address " (getRemoteAddress socketInfo) " has sent a keep-alive signal."))

(defn onKeyReceived
  [socketInfo data startIndex length]
  (println "Remote with address " (getRemoteAddress socketInfo) " has sent its sync-key.")
  (let [syncKey (String. data startIndex length)]
    (-> syncKey
      (addToPairs socketInfo)
      (get syncKey)
      (sendSyncStatus socketInfo))))

(defn onWaitingForSync
  [socketInfo]
  (println "Waiting for the other client to connect..."))



(defn onStartIgnoreSync
  [socketInfo]
  (println "Ready to start synchronization...")
  (let [data (FSMod/get-ignore-mod-info-bytes)]
    (FPClient/write socketInfo data (alength data))))


(defn onPassData
  [socketInfo data]
  (let [key (get-sync-key-from-data data)]
    (let [client (get-other-client socketInfo (getSyncPair key))]
      (Arrays/fill data 4 5 (byte 0x01))
      (FPClient/write client data (alength data)))))

(defn onIgnoreModInfoRecieved
  [socketInfo data]
  (->> data
    protocol/get-recieved-data
    (FSFilters/get-ignore-to-transfer (FSMod/get-ignore-mod-info))
    (send-ignore-file socketInfo)))


(defn onFileInfoRecieved
  [socketInfo data]
  (->> data
    protocol/get-recieved-data
    utils/from-bytes
    setIncomingFile)
  (FSAccess/remove-file ((getIncomingFile) :name)))

(defn onIgnoreRecieved
  [socketInfo data]
  (let [d (protocol/get-recieved-data data) info (getIncomingFile)]
    (with-open [outstream (FSAccess/get-file-output-stream (info :name))]
      (let [w (FSAccess/write-buffer-to-file outstream d (info :size) (info :recieved))]
        (swap! IncomingFile assoc :recieved w)
        (if (<= (info :size) w)
          (startFileSync socketInfo))))))

(defn onFileRecieved
  [socketInfo data]
  (let [d (protocol/get-recieved-data data) info (getIncomingFile)]
    (with-open [outstream (FSAccess/get-file-output-stream (info :name))]
      (let [w (FSAccess/write-buffer-to-file outstream d (info :size) (info :recieved))]
        (swap! IncomingFile assoc :recieved w)))))

(defn onFileModInfoRecieved
  [socketInfo data]
  (->> data
    protocol/get-recieved-data
    (FSFilters/get-files-to-transfer (FSMod/get-files-mod-info))
    (send-files socketInfo)))

(defn onAskDisconnect
  [socketInfo data]
  (let [key (get-sync-key-from-data data)]
    (let [client (get-other-client socketInfo (getSyncPair key))]
      (if (@Disconnects key)
        (do
          (disconnect socketInfo)
          (disconnect client)
          (remove-pair key)
          (remove-disc key))
        (set-client-disconnect key socketInfo)))))


(defn protocolReadHandler
  [socketInfo buffer bytesRead]
  (if (<= bytesRead 0)
    (onDisconnect socketInfo)
    (if (< bytesRead protocol/PROTOCOL_HEADER_LENGTH)
      (onError socketInfo ERROR_MALFORMED_HEADER)
      (let [totalLength (utils/bytesToInt buffer 0 false)
            dataLength (- totalLength protocol/PROTOCOL_HEADER_LENGTH)
            CW (utils/bytesToInt buffer 4 false)]
        (condp = CW
          protocol/CW_0000_NUM_ALLOW_DISC               (onAllowDisconnect          socketInfo)
          protocol/CW_0001_NUM_KEEP_ALIVE               (onKeepAlive                socketInfo)
          protocol/CW_0004_NUM_SYNC_KEY_SENT            (onKeyReceived              socketInfo buffer protocol/PROTOCOL_HEADER_LENGTH dataLength)
          protocol/CW_0005_NUM_SYNC_KEY_REC_WAITS       (onWaitingForSync           socketInfo)
          protocol/CW_0006_NUM_SYNC_KEY_REC_READY       (onStartIgnoreSync          socketInfo)
          protocol/CW_0007_NUM_IGNORE_MOD_INFO          (onPassData                 socketInfo buffer)
          protocol/CW_0007_NUM_IGNORE_MOD_INFO_REC      (onIgnoreModInfoRecieved    socketInfo buffer)
          protocol/CW_0008_NUM_IGNORE_FFILE_INFO        (onPassData                 socketInfo buffer)
          protocol/CW_0008_NUM_IGNORE_FFILE_INFO_REC    (onFileInfoRecieved         socketInfo buffer)
          protocol/CW_0009_NUM_IGNORE_FFILE             (onPassData                 socketInfo buffer)
          protocol/CW_0009_NUM_IGNORE_FFILE_REC         (onIgnoreRecieved           socketInfo buffer)
          protocol/CW_0010_NUM_FILE_MOD_INFO            (onPassData                 socketInfo buffer)
          protocol/CW_0010_NUM_FILE_MOD_INFO_REC        (onFileModInfoRecieved      socketInfo buffer)
          protocol/CW_0011_NUM_FFILE_INFO               (onPassData                 socketInfo buffer)
          protocol/CW_0011_NUM_FFILE_INFO_REC           (onFileInfoRecieved         socketInfo buffer)
          protocol/CW_0012_NUM_FFILE                    (onPassData                 socketInfo buffer)
          protocol/CW_0012_NUM_FFILE_REC                (onFileRecieved             socketInfo buffer)
          protocol/CW_0000_NUM_ASK_DISC                 (onAskDisconnect            socketInfo buffer)
          (onError socketInfo ERROR_CODE_WORD_UNKNOWN))))))

(defn queue
  ([] (clojure.lang.PersistentQueue/EMPTY))
  ([coll]
   (reduce conj clojure.lang.PersistentQueue/EMPTY coll)))
