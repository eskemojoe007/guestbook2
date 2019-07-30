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
  "Help function to pass messages through WS to server.
  Assumes that the ws-message is the first arg."
  [& args]
  (if-let [send-fn (:send-fn @socket)]
    (apply send-fn args)
    (throw (ex-info "Couldn't send message, channel isn't open"
                    {:message (first args)}))))

;;;; Define recieve message handling
(defmulti handle-message
  "Multi-method that recieves a ws-message and event."
  (fn [{:keys [id]} _] id))

(defmethod handle-message :message/add
  [_ msg-add-event]
  (rf/dispatch msg-add-event))

#_(defmethod handle-message :message/creation-errors
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
  (.log js/console "Event Recieved: " (pr-str id))
  (handle-message ws-message event))

(defstate channel-router
  "Creates a `go-loop` connecting incoming messages to the right functions"
  :start (sente/start-chsk-router! (:ch-recv @socket) #'receive-message!)
  :stop (when-let [stop-fn channel-router] (stop-fn)))
