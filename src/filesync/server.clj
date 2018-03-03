(ns filesync.server
  (:gen-class)
  (:require [filesync.sockets :as FPSocket]))

(require '[clojure.java.io :as io])
(import '[java.net ServerSocket])
(import '[java.net Socket])

(defn acceptClient
  [listener]
  (.accept listener))

(defn startAcceptingAsync
  [server readHandler]
  (let [serverSocket (server FPSocket/KEYWORD_SOCKET)
        keepListening (server FPSocket/KEYWORD_STAY_ALIVE)]
   (reset! keepListening true)
   (future
       (println "[Server] Starting to listen on a seperate thread...")
       (while @keepListening
         (println "[Server] Waiting for the next client to connect...")
         (-> serverSocket
           (acceptClient)
           (FPSocket/createSocketInfo)
           (FPSocket/printSocketInfo)
           (FPSocket/startReadingAsync readHandler)))))
  server)

(defn start
  [host port readHandler]
  (println "[MainThread] Started the application as a server instance.")
  (let [listener (ServerSocket. port)]
    (-> listener
      (FPSocket/createListenerInfo)
      (FPSocket/printListenerInfo)
      (startAcceptingAsync readHandler))))

(defn stopServer
  [server]
  (FPSocket/close server)
  server)
