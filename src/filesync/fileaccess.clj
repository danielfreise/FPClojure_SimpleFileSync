(ns filesync.fileaccess
  (:require [filesync.fileconfig        :as FSConfig])
  (:require [filesync.utils             :as FPUtils])
  (:require [filesync.inputvalidation   :as FPInput])
  (:require [clojure.java.io            :as IO])
  (:require [filesync.protocol          :as protocol])
  (:gen-class))

(import '[java.io FileOutputStream]
        '[java.util Date])

(defn file-exists? [name] 
  "Check if file exists."
  (->> name
    IO/as-file
    .exists))

(defn get-file [name] 
  "Returns the given file." 
  (IO/as-file name)) 
  
(defn get-all-files [dir] 
  "Returns a sequence of all files in a diretory sequence." 
  (->> dir 
    IO/file 
    file-seq 
    (filter #(.isFile %)))) 

(defn get-config []
  "load configuration from .syncconf file."
  "Returns a map containing the configuration."
  (if (file-exists? FSConfig/CONFIG_FILE_NAME)
    (->> FSConfig/CONFIG_FILE_NAME
      slurp
      clojure.edn/read-string)
    nil))

(defn get-config-args []  
  (FPUtils/convertToHashmap (get-config))) 

(defn get-sync-key-bytes []  
  ((get-config-args) FPInput/KEYWORD_SYNCKEY)) 

(defn get-sync-key-transfer []
  (protocol/get-data-to-send protocol/CW_0004_BYTES_SYNC_KEY_SENT (get-sync-key-bytes) (byte-array 0)))
 
(defn get-transfer-info [filename]
  "Returns information about the file that should be transfered."
  (let [info {:name filename}]
    (assoc info :size (.length (IO/as-file (str ((get-config-args) :-filePath) filename))))))

(defn get-file-byte-stream [filename]
  "Returns a stream of the files contents."
  (IO/input-stream (str ((get-config-args) :-filePath) filename)))

(defn get-file-output-stream [filename]
  "Returns a  output stream for the file."
  (FileOutputStream. (str ((get-config-args) :-filePath) filename) true))

(defn read-stream [stream buffer]
  "Fills the buffer with data from the stream."
  (let [l (alength buffer)]
    (.read stream buffer 0 l)))

(defn remove-file [filename]
  "Remove a file."
  (if (file-exists? (str ((get-config-args) :-filePath) filename))
    (IO/delete-file (str ((get-config-args) :-filePath) filename))))

(defn get-bytes-to-write [length written buffer]
  "Returns the number of bytes to write."
  (let [blen (alength buffer)]
    (if (< length (+ written blen))
      (- length written)
      blen)))

(defn write-buffer-to-file [stream buffer length written]
  "Append bytes of a buffer to a file (creates file if it does not exist)."
    (let [len (get-bytes-to-write length written buffer)]
      (.write stream buffer 0 len)
      len))
