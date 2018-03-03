(ns filesync.protocol
  (:gen-class)
  (:require [filesync.utils :as FPUtils]))

(import '[java.util Arrays])

; Protocol (Common):
; [4 BYTES: TOTAL LENGTH] [4 BYTES: PROTOCOL CODE WORDS]
(def ^:const PROTOCOL_HEADER_LENGTH (int 8))
(def ^:const SYNC_KEY_LENGTH (int 8))
(def ^:const READ_BUFFER_SIZE (int 1024))
(def ^:const BUFFER_SIZE (- (- READ_BUFFER_SIZE PROTOCOL_HEADER_LENGTH) SYNC_KEY_LENGTH))

; Code Words:
; 0x 0000 0000: allow to end communication gracefully
(def ^:const CW_0000_NUM_ALLOW_DISC (long 0x00000000))
(def CW_0000_BYTES_ALLOW_DISC (byte-array [0 0 0 0]))

(def ^:const CW_0002_NUM_FORCE_DISC (long 0x00000002))
(def CW_0002_BYTES_FORCE_DISC (byte-array [0 0 0 2]))

(def ^:const CW_0000_NUM_ASK_DISC (long 0x00000003))
(def CW_0000_BYTES_ASK_DISC (byte-array [0 0 0 3]))

; 0x 0000 0001: keep alive / keep waiting
(def ^:const CW_0001_NUM_KEEP_ALIVE (long 0x00000001))
(def CW_0001_BYTES_KEEP_ALIVE (byte-array  [0 0 0 1]))

; 0x 0000 0002: sync client has disconnected (normal disconnect)
;(def ^:const CW_0002_SYNC_DISC_NUM (long 0x00000002))
;(def CW_0002_SYNC_DISC  (byte-array  [0 0 0 2]))

; 0x 0000 0003: sync client has been lost (unexpected loss of connection)
;(def ^:const CW_0003_SYNC_LOST_NUM (long 0x00000003))
;(def CW_0003_SYNC_LOST  (byte-array  [0 0 0 3]))

; 0x 0000 0010: indicates incoming sync key (sent by client)
(def ^:const CW_0004_NUM_SYNC_KEY_SENT      (long 0x00000004))
(def CW_0004_BYTES_SYNC_KEY_SENT      (byte-array [0 0 0 4]))
; 0x 0000 0011: indicates sync key has been received + waiting for syncKey
(def ^:const CW_0005_NUM_SYNC_KEY_REC_WAITS (long 0x00000005))
(def CW_0005_BYTES_SYNC_KEY_REC_WAITS (byte-array [0 0 0 5]))
; 0x 0000 0012: indicates sync key has been received + ready for transfer
(def ^:const CW_0006_NUM_SYNC_KEY_REC_READY (long 0x00000006))
(def CW_0006_BYTES_SYNC_KEY_REC_READY (byte-array [0 0 0 6]))

(def BYTES_MIN_HEADER_LENGTH (FPUtils/intToBytes 8 (byte-array 4) 0))

(def MSG_WAITS_FOR_SYNC   (byte-array (concat BYTES_MIN_HEADER_LENGTH CW_0005_BYTES_SYNC_KEY_REC_WAITS)))

(def MSG_READY_FOR_SYNC   (byte-array (concat BYTES_MIN_HEADER_LENGTH CW_0006_BYTES_SYNC_KEY_REC_READY)))

(def MSG_TOO_MANY_CLIENTS (byte-array (concat BYTES_MIN_HEADER_LENGTH CW_0002_BYTES_FORCE_DISC)))

(def MSG_COM_END          (byte-array (concat BYTES_MIN_HEADER_LENGTH CW_0000_BYTES_ALLOW_DISC)))

(def MSG_ASK_DISC         (byte-array (concat BYTES_MIN_HEADER_LENGTH CW_0000_BYTES_ASK_DISC)))

; sync
;ignore
;ignore send
(def ^:const CW_0007_NUM_IGNORE_MOD_INFO (long 0x00000007))
(def CW_0007_BYTES_IGNORE_MOD_INFO (byte-array [0 0 0 7]))

;ignore recieved
(def ^:const CW_0007_NUM_IGNORE_MOD_INFO_REC (long 0x01000007))

;ignore file info send
(def ^:const CW_0008_NUM_IGNORE_FFILE_INFO (long 0x00000008))
(def CW_0008_BYTES_IGNORE_FILE_INFO (byte-array [0 0 0 8]))

;ignore file info recieved
(def ^:const CW_0008_NUM_IGNORE_FFILE_INFO_REC (long 0x01000008))

;ignore file send
(def ^:const CW_0009_NUM_IGNORE_FFILE (long 0x00000009))
(def CW_0009_BYTES_IGNORE_FILE (byte-array [0 0 0 9]))

;ignore file recieved
(def ^:const CW_0009_NUM_IGNORE_FFILE_REC (long 0x01000009))


;data
;data mod info send
(def ^:const CW_0010_NUM_FILE_MOD_INFO (long 0x0000000a))
(def CW_0010_BYTES_FILE_MOD_INFO (byte-array [0 0 0 10]))

;data mod info recieved
(def ^:const CW_0010_NUM_FILE_MOD_INFO_REC (long 0x0100000a))

;file info send
(def ^:const CW_0011_NUM_FFILE_INFO (long 0x0000000b))
(def CW_0011_BYTES_FILE_INFO (byte-array [0 0 0 11]))

;file info recieved
(def ^:const CW_0011_NUM_FFILE_INFO_REC (long 0x0100000b))

;file send
(def ^:const CW_0012_NUM_FFILE (long 0x0000000c))
(def CW_0012_BYTES_FILE (byte-array [0 0 0 12]))

;file recieved
(def ^:const CW_0012_NUM_FFILE_REC (long 0x0100000c))

(defn get-data-to-send [CW sync-key data]
  (let [header-len    PROTOCOL_HEADER_LENGTH
        key-len       SYNC_KEY_LENGTH
        data-len      (alength data)]
    (-> (+ header-len key-len data-len)
      (FPUtils/intToBytes (byte-array 4) 0)
      (concat CW sync-key data)
      byte-array)))


(defn get-recieved-data [data]
  (let [len (+ PROTOCOL_HEADER_LENGTH SYNC_KEY_LENGTH)]
    (Arrays/copyOfRange data len (alength data))))
