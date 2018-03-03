(ns filesync.core
  (:gen-class)
  (:require [filesync.utils           :as FPUtils])
  (:require [filesync.client          :as FPClient])
  (:require [filesync.server          :as FPServer])
  (:require [filesync.sockets         :as FPSocket])
  (:require [filesync.inputvalidation :as FPInput])
  (:require [filesync.protocolHandler :as FPApp])
  (:require [filesync.fileaccess      :as FSAccess])
  (:require [clojure.string :as str]))

(defn networkConfigure
  "Allows to add / override configuration values and callbacks, such as the protocolHandler."
  ([argmap k v]
   (if (FPUtils/containsError argmap)
    argmap
    (assoc argmap k v)))

  ([argmap message k v]
   (println message)
   (networkConfigure k v)))

(defn networkStartUp
  "Initializes the network, i.e examines the mode and starts a server or client accordingly."
  ([argmap]
   (if (FPUtils/containsError argmap)
     argmap
     (let [mode     (argmap FPInput/KEYWORD_MODE)
           host     (argmap FPInput/KEYWORD_HOST)
           port     (argmap FPInput/KEYWORD_PORT)
           handler  (argmap FPInput/KEYWORD_HANDLER)]
       (if (= mode "server")
         (->> handler
           (FPServer/start host port)
           (assoc argmap FPSocket/KEYWORD_SERVER))
         (->> handler
           (FPClient/start host port)
           (assoc argmap FPSocket/KEYWORD_CLIENT))))))

  ([argmap message]
   (println message)
   (networkStartUp argmap)))


(defn networkInitiateCommunication
  ([argmap]
   (if-not (or (FPUtils/containsError argmap) (nil? (argmap FPSocket/KEYWORD_CLIENT)))
     (let [data (FSAccess/get-sync-key-transfer)]
      (FPClient/write (argmap FPSocket/KEYWORD_CLIENT) data (alength data))))
   argmap)

  ([argmap message]
   (println message)
   (networkInitiateCommunication argmap)))


(defn networkKeepAlive
  ([argmap]
   (if (FPUtils/containsError argmap)
     argmap
     (if (nil? (argmap FPInput/KEYWORD_KEEPALIVE))
       (assoc argmap FPUtils/KEYWORD_WARNING "Missing argmap or keep alive value.")
       (do
         (Thread/sleep (argmap FPInput/KEYWORD_KEEPALIVE))
         argmap))))

 ([argmap message]
  (println message)
  (networkKeepAlive argmap)))


(defn networkShutdown
  ([argmap]
   (if-not (FPUtils/containsError argmap)
     (if (nil? (argmap :-client))
       (FPServer/stopServer (argmap :-server))
       (FPClient/stopClient (argmap :-client))))
   argmap)

  ([argmap message]
   (println message)
   (networkShutdown argmap)))


(defn validateInput
  "Handles the input provided in the args. Uses multiple validations to check the number of arguments
   and validates the relevant entries. Returns a map that contains the arguments and the first error that occured."
  ([args]
   (if (FPUtils/zeroOrOdd (count args))
     ({:-error ""})
     (-> args
       (FPUtils/convertToHashmap)
       (FPInput/validateMode)
       (FPInput/validateHost)
       (FPInput/validatePort))))

  ([args message]
   (println message)
   (validateInput args)))

; ##### ENTRY POINT #####
(defn -main
  "Entry point for the application."
  []
  (-> (FSAccess/get-config)
    (validateInput      "[MainThread] Checking input arguments.")
    (networkConfigure   FPInput/KEYWORD_HANDLER FPApp/protocolReadHandler)
    (networkStartUp     "[MainThread] Initializing the network.")
    (networkInitiateCommunication)
    (networkKeepAlive   "[MainThread] Sleeping - Keeping the application alive.")
    (networkShutdown    "[MainThread] Shutting down the network.")))

;(-main)
