(ns guestbook2.websockets
  (:require-macros [mount.core :refer [defstate]])
  (:require [taoensso.sente :as sente]
            [re-frame.core :as rf]
            mount.core))

;;;; Initialize sente on the front end
;; Each front end gets its own sente socket instance
(defstate socket
  "Socket is the websocket made with sente"
  :start (sente/make-channel-socket!
          "/ws"
          (.-value (.getElementById js/document "token"))
          {:type :auto :wrap-recv-evs? false}))

;; Create helper function to send
(defn send!
  [message]
  (if-let [send-fn (:send-fn @socket)]
    (send-fn message)
    (throw (ex-info "Couldn't send message, channel isn't open"
                    {:message message}))))

;;;; Define recieve message handling
(defmulti handle-message (fn [{:keys [id]} _] id))

(defmethod handle-message :message/add
  [_ msg-add-event]
  (rf/dispatch msg-add-event))

(defmethod handle-message :message/creation-errors
  [_ [_ response]]
  (rf/dispatch [:form/set-server-errors (:errors response)]))

;; Some basic types of sente's that will happen.
(defmethod handle-message :chsk/handshake
  [{:keys [event]} _]
  (.log js/console "Connection Established: " (pr-str event)))
(defmethod handle-message :chsk/state
  [{:keys [event]} _]
  (.log js/console "State Changed: " (pr-str event)))

(defmethod handle-message :default
  [{:keys [event]}]
  (.log js/console "Received unrecognized websocket event type: " (pr-str event)))

(defn receive-message!
  "Receives the ws-message and extracts the proper info"
  [{:keys [id event] :as ws-message}]
  (.log js/console "Event Recieved: " (pr-str event))
  (handle-message ws-message event))

(defstate channel-router
  "Creates a `go-loop` connecting incoming messages to the right functions"
  :start (sente/start-chsk-router! (:ch-recv @socket) #'receive-message!)
  :stop (when-let [stop-fn channel-router] (stop-fn)))
