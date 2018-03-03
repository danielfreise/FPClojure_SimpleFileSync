(ns filesync.utils
  (:gen-class))

(import '[java.util Arrays])

(def ^:const KEYWORD_INFO     (keyword "-info"))
(def ^:const KEYWORD_WARNING  (keyword "-warning"))
(def KEYWORD_ERROR            (keyword "-error"))

; Temporary solutions / helper
(defn return
  "Helper method to easily spot points of return in complex functions. Serves no other use and will eventually be removed"
  [arg]
  arg)

; Actual utility functions
(defn containsError
  "Checks whether the specified map contains a value for the :-error keyword."
  [argmap]
  (not (nil? (argmap (keyword KEYWORD_ERROR)))))

(defn zeroOrOdd
  "Checks whether the number is zero or odd."
  [num]
  (or (== num 0) (odd? num)))

; Is this inefficient?
; - first paritions a list (key01 val01 key02 val02 ...) into ((key01 val01) (key02 val02) ...)
; - then creates a list of maps based on pairings ({:key01 val01} {:key02 val02})
; - then merges the seperate maps into a big map...
; If enough time => revision/refactor?
(defn convertToHashmap
  "Converts the arguments (sequence) to a hash-map and returns the result."
  [argseq]
  (let [arglist (partition 2 argseq)
        argmaps (map #(hash-map (keyword (first %1)) (second %1)) arglist)]
      (apply merge argmaps)))

(defn areArraysPartiallySame
  "Determines whether the arrays (lhs and rhs) contain the same elements from start to start + length."
  [lhs rhs start length]
  (if (or (nil? lhs) (nil? rhs) (< start 0) (<= length 0))
    false
    (let [len_lhs (alength lhs)
          len_rhs (alength rhs)
          len_req (+ start length)]
      (if (or (< len_lhs len_req) (< len_rhs len_req))
        false
        (loop [index_lhs start
               index_rhs 0
               equal true]
          (if (and (< index_lhs len_req) equal)
            (recur (inc index_lhs) (inc index_rhs) (== (aget lhs index_lhs) (aget rhs index_rhs)))
            equal))))))

; ##### NUM CONVERSIONS #####
(def ^:const MAX_UBYTE (int 0xFF))

(def ^:const MIN_INT32  (unchecked-long 0xFFFFFFFF80000000))
(def ^:const MAX_INT32  (unchecked-long 0x000000007FFFFFFF))
(def ^:const SIZEOF_INT32   4)
(def ^:const BITS_PER_BYTE  8)

(defn bytesToInt
  [buffer start signed]
  (if (or (nil? buffer) (< start 0) (> (+ start SIZEOF_INT32) (alength buffer)))
    (return nil)
    (let [invert #(if (and %1 (> %2 MAX_INT32)) (long (unchecked-int %2)) %2)]
      (loop [value (long 0)
             index start
             shiftingBits (* 3 BITS_PER_BYTE)]
        (if-not (< shiftingBits 0)
          (recur (-> buffer
                   (aget index)
                   (bit-and MAX_UBYTE)
                   (bit-shift-left shiftingBits)
                   (bit-or value))
                 (inc index)
                 (- shiftingBits BITS_PER_BYTE))
          (invert signed value))))))

(defn intToBytes
  [value buffer start]
  (if (or (nil? buffer) (< start 0) (> (+ start SIZEOF_INT32) (alength buffer)))
    nil
    (loop [shiftingBits (* 3 BITS_PER_BYTE)
           index start]
      (if-not (< shiftingBits 0)
        (do
          (aset buffer index (unchecked-byte (bit-shift-right value shiftingBits)))
          (recur (- shiftingBits BITS_PER_BYTE) (inc index)))
        buffer))))

(defn to-bytes [el]
  (.getBytes (str el) "UTF-8"))

(defn from-bytes [el]
  (clojure.edn/read-string (String. el "UTF-8")))
