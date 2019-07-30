(ns guestbook2.routes.websockets
  (:require [clojure.tools.logging :as log]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.immutant :refer [get-sch-adapter]]
            [mount.core :refer [defstate]]
            [guestbook2.messages :as msg]
            [guestbook2.middleware :as middleware]))
            ; [java-time :as time]))

;; Initialize sente
(defstate socket
  "Create the socket state using sente"
  :start (sente/make-channel-socket!
          (get-sch-adapter)

          ;; Set user-id-fn since we don't have uid in sessions yet.
          ;; TODO: When adding users, remove this and use uid
          {:user-id-fn (fn [ring-req]
                         (get-in ring-req [:params :client-id]))}))

;; Create helper function - send
(defn send!
  "Funtion used to send messages through the socket"
  [uid message]
  ; (:send-fn socket) gets the send function from the init of the socket
  ((:send-fn socket) uid message))


;;;; Define Receive message handling

;; handle-message - multimethod to handle multiple types of inputs
(defmulti handle-message
  "Multi Function that takes a ws-message and tries to handle it."
  (fn [{:keys [id]}] id))

(defmethod handle-message :default
  [{:keys [id]}]
  ; (log/debug "Received unrecognized websocket event type: " id))
  ;; We want to return an error object
  {:errors {:server-error [(str "Unreocgnized websocket event type: " (pr-str id))]}})
  ; {:error (str "Unrecognized websocket event type: " (pr-str id))
  ;  :id id})

(defmethod handle-message :message/create!
  [{:keys [?data uid] :as message}]
  (let [response (try
                   (msg/save-message! ?data)
                   (assoc ?data :timestamp (java.util.Date.))
                   (catch Exception e
                     (let [{id :guestbook2/error-id
                            errors :errors} (ex-data e)]
                       (case id
                         :validation
                         {:errors errors}
                         ;; else case - Simple server error map
                         {:errors {:server-error ["Failed to save message!"]}}))))]
    (if (:errors response)
      response
      ; (send! uid [:message/creation-errors response])
      ; No errors - Then we get the UID of the `any` connections and send it
      (do
        (doseq [push-uid (-> socket
                            :connected-uids
                            deref
                            :any)]
          (send! push-uid [:message/add response]))
        {:success true}))))

(defn receive-message!
  "Global actions for all types of messages."
  [{:keys [id ?reply-fn] :as message}]
  (log/debug "Got message with id: " id)
  (let [reply-fn (or ?reply-fn (fn [_]))]
    ;; Run the function and save the response when it exists
    (when-some [response (handle-message message)]
      (log/debug "response: " (pr-str response))
      (reply-fn response))))

;;;; Create Routers

;; channel-router connects our sente socket to incoming messages
;; has to be a defstate so mount loads it after it makes socket
(defstate channel-router
  "Creates a `go-loop` connecting incoming messages to the right functions"
  :start (sente/start-chsk-router! (:ch-recv socket) #'receive-message!)
  :stop (when-let [stop-fn channel-router] (stop-fn)))

;; Backup ajax if ws doesn't work.
(defn websocket-routes
  []
  ["/ws" {:middleware [middleware/wrap-csrf
                       middleware/wrap-formats]
          :get (:ajax-get-or-ws-handshake-fn socket)
          :post (:ajax-post-fn socket)}])
