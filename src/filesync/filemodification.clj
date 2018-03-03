(ns filesync.filemodification 
  (:require [filesync.fileconfig    :as FSConfig]) 
  (:require [filesync.fileaccess    :as FSAccess]) 
  (:require [filesync.filefilters   :as FSFilters]) 
  (:require [filesync.utils         :as FPUtils]) 
  (:require [clojure.java.io        :as IO])
  (:require [clojure.string         :as str])
  (:require [filesync.protocol      :as protocol])
  (:gen-class)) 
 
(import '[java.util Date]) 
 
(defn get-file-modification-date [file] 
  "Returns modification timestamp of a file" 
  (->> file 
    .lastModified 
    Date. 
    .getTime)) 
 
(defn get-file-name [sync-root file] 
  "Returns the name of a file" 
  (-> file 
    .getPath 
    (str/replace sync-root ""))) 
 
(defn create-file-modification-map [sync-root file] 
  "Map of filename and its modification timestamp" 
  (let [mod {}] 
    (assoc mod  
      :name (get-file-name sync-root file) 
      :time (get-file-modification-date file)))) 
 
(defn get-file-modification-dates [sync-root files] 
  "Returns an array containing the modification info of the files." 
  (loop [fs files result []] 
    (if fs 
      (let [f (first fs)]
        (if f
          (recur (next fs) (conj result (create-file-modification-map sync-root f))))) 
      result))) 
 
(defn get-sync-root []
  (->> (FSAccess/get-config-args)
    :-filePath))

(defn get-ignore-mod-info [] 
  "Returns file modification info for the ignore file." 
  (let [sync-root (get-sync-root)]
    (let [ignore (str sync-root FSConfig/IGNORE_FILE_NAME)] 
      (if (FSAccess/file-exists? ignore) 
        (->> ignore 
          FSAccess/get-file 
          file-seq 
          (get-file-modification-dates sync-root)) 
        [{:name (FSFilters/get-relative-filename-path sync-root ignore) :time -1}]))))

(defn get-files-mod-info [] 
  "Returns file modification info for the ignore file."
  (let [sync-root (get-sync-root)]
    (->> sync-root 
      FSAccess/get-all-files
      (filter #(FSFilters/filter-ignored-files % sync-root))
      (get-file-modification-dates sync-root))))
 
(defn get-ignore-mod-info-bytes [] 
  "Returns the bytes of the ignore file modification info (and header)." 
  (let [CW protocol/CW_0007_BYTES_IGNORE_MOD_INFO key (FSAccess/get-sync-key-bytes)]
    (->> (get-ignore-mod-info)
      FPUtils/to-bytes
      (protocol/get-data-to-send CW key))))

(defn get-files-mod-info-bytes [] 
  "Returns the bytes of the file modification info (and header)." 
  (let [CW protocol/CW_0010_BYTES_FILE_MOD_INFO key (FSAccess/get-sync-key-bytes)]
    (->> (get-files-mod-info)
      FPUtils/to-bytes
      (protocol/get-data-to-send CW key))))
