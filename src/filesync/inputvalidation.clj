(ns filesync.inputvalidation
  (:gen-class)
  (:require [filesync.utils :as FPUtils]))

(def VAL_LOCALHOST "localhost")

; ##### Global defintions #####
(def INFO_NO_ERROR "INFO: Executed input validtion without any errors.")

(def ERROR_NUM_ARGS "Error: Number of arguments must neither be zero nor odd.")

(def ERROR_NO_MODE "Error: No mode specified. Please specify a mode on startup (e.g. -mode server).")
(def ERROR_NO_HOST "Error: No host specified. Please specify a host on startup (e.g. -host localhost).")
(def ERROR_NO_PORT "Error: No port specified. Please specify a port on startup (e.g. -port 50000).")

(def ERROR_INVALID_MODE "Error: Invalid mode specified. Please specify a valid mode: [client, server]")
(def ERROR_INVALID_HOST "Error: Invalid host specified. Please specify a valid host: [localhost, ...]")
(def ERROR_INVALID_PORT "Error: Invalid port specified. Please specify a valid port: [server: 0 - 65535, client: 1 - 65535]")

(def ^:const KEYWORD_HOST       (keyword "-host"))
(def ^:const KEYWORD_PORT       (keyword "-port"))
(def ^:const KEYWORD_MODE       (keyword "-mode"))
(def ^:const KEYWORD_SYNCKEY    (keyword "-syncKey"))
(def ^:const KEYWORD_HANDLER    (keyword "-handler"))
(def ^:const KEYWORD_KEEPALIVE  (keyword "-keepAlive"))

; ##### Input Validation #####
(defn validatePort
  "Validates the specified port. The port number must be an integer x so that x > 0 && x < 65336.
   [TODO: Servers can actually be started using port 0, string parsing should be available.]"
  [argmap]
  (if (FPUtils/containsError argmap)
    argmap
    (let [port (argmap KEYWORD_PORT)]
      (if (nil? port)
        (assoc argmap FPUtils/KEYWORD_ERROR ERROR_NO_PORT)
        (if-not (and (integer? port) (< 0 port) (> 65356 port))
          (assoc argmap FPUtils/KEYWORD_ERROR ERROR_INVALID_PORT)
          argmap)))))

(defn validateHost
  "Validates the specified host. Currently, the function only checks for the host being exactly 'localhost'
   as thi is enough for the test environment."
  [argmap]
  (if (FPUtils/containsError argmap)
    argmap
    (let [host (argmap KEYWORD_HOST)]
      (if (nil? host)
        (assoc argmap FPUtils/KEYWORD_ERROR ERROR_NO_HOST)
        ; add more validation, for now only accept "localhost"
        (if (= host VAL_LOCALHOST)
          argmap
          (assoc argmap FPUtils/KEYWORD_ERROR ERROR_INVALID_HOST))))))

(defn validateMode
  "Validates the specified mode. The mode can either be 'client' or 'server'."
  [argmap]
  (if (FPUtils/containsError argmap)
    argmap
    (let [mode (argmap KEYWORD_MODE)]
      (condp = mode
        "server" argmap
        "client" argmap
        (assoc argmap FPUtils/KEYWORD_ERROR ERROR_INVALID_MODE)))))

; This might be removed, as it is currently no longer needed.
(defn completeValidation
  "Sets a final information using the :-error keyword that indicates a successful validation.
   This function should be the last in the validation chain, if used at all."
  [argmap]
  (if (FPUtils/containsError argmap)
    argmap
    (assoc argmap FPUtils/KEYWORD_INFO INFO_NO_ERROR)))


; TODO: string parsing for the port number as this might also be provided as string input (e.g. cmd)
; TODO: host parsing (ip address in different formats???)
