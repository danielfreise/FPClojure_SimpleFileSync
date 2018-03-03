(ns filesync.filefilters 
  (:require [filesync.fileconfig  :as FSConfig]) 
  (:require [clojure.java.io      :as IO])
  (:require [filesync.fileaccess  :as FSAccess])
  (:require [filesync.utils       :as FPUtils])
  (:require [clojure.string       :as str])
  (:gen-class))

  (import '[java.util Date])

;; send filter
(defn get-other-mod-time [filename other-info]
  "Returns the modification time of a file in a file-info-array by its filename."
  (loop [fs other-info]
    (if fs
      (let [f (first fs)]
        (if (= filename (f :name))
          (f :time)
          (recur (next fs))))
      -1)))

(defn get-current-time []
  (->> (Date.)
    .getTime))

(defn get-last-sync []
  (let [sync-root ((FSAccess/get-config-args) :-filePath)]
    (let [last (str sync-root FSConfig/LAST_SYNC_NAME)]
      (if (FSAccess/file-exists? last)
        (->> last
          slurp
          clojure.edn/read-string)
        -1))))

(defn store-last-sync []
  (let [sync-root ((FSAccess/get-config-args) :-filePath)]
    (->> (get-current-time)
      (spit (str sync-root FSConfig/LAST_SYNC_NAME)))))
        

(defn filter-files-to-send [file-info other-info]
  "Filterfunction that checks if a file is new on the local client.
    Returns true if the newer File is on the local client."
  (let [newer         (> (file-info :time) (get-other-mod-time (file-info :name) other-info))
        after-last    (> (file-info :time) (get-last-sync))]
    (and newer after-last)))

(defn filter-files-to-send-simple [file-info other-info]
  "Filterfunction that checks if a file is new on the local client.
    Returns true if the newer File is on the local client."
  (> (file-info :time) (get-other-mod-time (file-info :name) other-info)))

(defn get-files-to-transfer [my-mod-info mod-bytes]
  "Returns a list of files that should be transfered to the other client."
  (let [other-mod-info (FPUtils/from-bytes mod-bytes)]
    (->> my-mod-info
      (filter #(filter-files-to-send % other-mod-info)))))

(defn get-ignore-to-transfer [my-mod-info mod-bytes]
  "Returns a list of files that should be transfered to the other client."
  (let [other-mod-info (FPUtils/from-bytes mod-bytes)]
    (->> my-mod-info
      (filter #(filter-files-to-send-simple % other-mod-info)))))

;; ignored files
(defn ignore-exists [file-name]
  "Returns true if the ignore file exists."
  (->> file-name
    IO/as-file
    .exists))

(defn get-ignores [file-name]
  "Returns a vector of ignore patterns from the ignore file."
  (if (ignore-exists file-name)
    (-> file-name
      slurp
      str/split-lines
      (conj FSConfig/IGNORE_FILE_NAME)
      (conj FSConfig/CONFIG_FILE_NAME))
    []))

(defn get-relative-file-path [sync-root file]
  "Returns the relative path of a file."
  (-> file
    .getPath
    (str/replace sync-root "")))

(defn get-relative-filename-path [sync-root filename]
  "Returns the relative path of a file."
  (-> filename
    (str/replace sync-root "")))
  
(defn replace-slashes [path]
  "Replaces slashes by baskslashes."
  (str/replace path "\\" "/"))

(defn match-ignore [ignore path]
  (->> path
    replace-slashes
    (re-matches (re-pattern ignore))))

(defn match-file-to-ignores [ignores filepath]
  (some #(match-ignore % filepath) ignores))

(defn file-ignored? [ignores filepath]
  "Params: ignore vector, relative filepath"
  "Returns false if a file has to be ignored."
  (if (nil? (match-file-to-ignores ignores filepath))
    true
    false))

(defn filter-ignored-files [file sync-root]
  "Filterfunction that checks if a file is ignored.
   Returns true if the file is not ignored."
  (let [ignores (get-ignores (str sync-root FSConfig/IGNORE_FILE_NAME))]
    (->> file
      (get-relative-file-path sync-root)
      (file-ignored? ignores))))