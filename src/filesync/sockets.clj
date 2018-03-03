(ns filesync.sockets
  (:gen-class)
  (:require [clojure.java.io        :as IO])
  (:require [filesync.protocol      :as protocol])
  (:import [java.net.Socket]))

(def ^:const KEYWORD_SOCKET       (keyword "-socket"))
(def ^:const KEYWORD_STREAM_IN    (keyword "-streamIn"))
(def ^:const KEYWORD_STREAM_OUT   (keyword "-streamOut"))

(def ^:const KEYWORD_LOCAL_IP     (keyword "-localIP"))
(def ^:const KEYWORD_LOCAL_PORT   (keyword "-localPort"))
(def ^:const KEYWORD_REMOTE_IP    (keyword "-remoteIP"))
(def ^:const KEYWORD_REMOTE_PORT  (keyword "-remotePort"))

(def ^:const KEYWORD_STAY_ALIVE   (keyword "-stayAlive"))
(def ^:const KEYWORD_CLIENT       (keyword "-client"))
(def ^:const KEYWORD_SERVER       (keyword "-server"))

; ##### CREATION OF SOCKET INFORMATION #####
(defn createSocketInfo
  [socket]
  (println "Gathering socket information.")
  { KEYWORD_SOCKET      socket
    KEYWORD_STREAM_OUT  (-> socket (IO/output-stream))
    KEYWORD_STREAM_IN   (-> socket (IO/input-stream))
    KEYWORD_LOCAL_IP    (-> socket (.getLocalAddress) (.getHostAddress))
    KEYWORD_LOCAL_PORT  (-> socket (.getLocalPort)) ;
    KEYWORD_REMOTE_IP   (-> socket (.getInetAddress) (.getHostAddress))
    KEYWORD_REMOTE_PORT (-> socket (.getPort))
    KEYWORD_STAY_ALIVE  (atom false)})

(defn createListenerInfo
  [serverSocket]
  (println "Gathering server socket (listener) information.")
  { KEYWORD_SOCKET serverSocket
    KEYWORD_LOCAL_IP    (-> serverSocket (.getInetAddress) (.getHostAddress))
    KEYWORD_LOCAL_PORT  (-> serverSocket (.getLocalPort))
    KEYWORD_STAY_ALIVE  (atom false)})

; ##### PRINTING SOCKT INFORMATION #####
(defn printSocketInfo
  [socketInfo]
  (println "########## Socket ##########"
    "\nLocal: "   (socketInfo KEYWORD_LOCAL_IP)   ":" (socketInfo KEYWORD_LOCAL_PORT)
    "\nRemote: "  (socketInfo KEYWORD_REMOTE_IP)  ":" (socketInfo KEYWORD_REMOTE_PORT))
  socketInfo)

(defn printListenerInfo
  [serverSocketInfo]
  (println "########## Server Socket (Listner) ##########"
    "\nIP: " (serverSocketInfo KEYWORD_LOCAL_IP) ":" (serverSocketInfo KEYWORD_LOCAL_PORT))
  serverSocketInfo)

; ##### SOCKET / LISTENER OPERATIONS #####
(defn readFromStream
  "Reads from the specified stream and writes the data to the supplied buffer."
  [stream buffer]
  (.read stream buffer 0 (alength buffer)))

(defn writeToStream
  "Writes to the specified stream from the supplied buffer.
   Passing the length of 0 skips writing and simply returns straight away."
  [stream buffer length]
  (.write stream buffer 0 length)
  (.flush stream)
  length)

(defn startReadingAsync
  "Starts reading asynchronously using the specified socket information.
   Received data will be processReceivedData by the supplied protocolHandler."
  [socketInfo protocolReadHandler]
  (future
    (println "[Socket] Starting to read asynchronously.")
    (let [streamIn  (socketInfo KEYWORD_STREAM_IN)
          bufferIn  (byte-array protocol/READ_BUFFER_SIZE)
          stayAlive (socketInfo KEYWORD_STAY_ALIVE)]
      (reset! stayAlive true)
      (try
        (while @stayAlive
          (->> bufferIn
            (readFromStream streamIn)
            (protocolReadHandler socketInfo bufferIn)))
        (catch Exception e
          (if @stayAlive
            (do
              (println "Exception occured: \n")
              (.printStackTrace e)))))))
  socketInfo)

(defn close
  "Takes (server) socket information created by corresponding methods, resets
   atomic state and closes the associated socket instances."
  [socketInfo]
  (reset! (socketInfo KEYWORD_STAY_ALIVE) false)
  (.close (socketInfo KEYWORD_SOCKET))
  socketInfo)
