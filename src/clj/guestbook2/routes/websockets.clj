(ns guestbook2.routes.websockets
  (:require [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [clojure.edn :as edn]
            [guestbook2.messages :as msg]
            [java-time :as time]))

;; Create a channels attom with a set
(defonce channels (atom #{}))

(defn connect!
  "This function adds a connection to the channels atom."
  [channel]
  (log/info "Channel Open")
  (swap! channels conj channel))

(defn disconnect!
  "This function removes a connection with reason from the channels atom."
  [channel {:keys [code reason]}]
  (log/info "Channel closed. close code: " code " reason: " reason)
  (swap! channels disj channel))

(defn handle-message!
  [channel ws-message]
  (let [message (edn/read-string ws-message)
        response (try
                   ;; Save the message and return ok with extra ok status.
                   (msg/save-message! message)
                   (assoc message :timestamp (time/local-date-time))
                   ; (ok {:status :ok})
                   (catch Exception e
                     ;; this line is using destructuring from the (ex-data e)
                     ;; to get the id and actual errors set by save-message!
                     (let [{id :guestbook2/error-id
                            errors :errors} (ex-data e)]

                       ;; Depending on ID we'll do different responses
                       (case id
                         :validation
                         {:errors errors}
                         ; (bad-request {:errors errors})
                         ;; else case - Simple server error map
                         ; (internal-server-error
                         {:errors {:server-error ["Failed to save message!"]}}))))]
    (if (:errors response)
      (async/send! channel (pr-str response))
      (doseq [channel @channels]
        (async/send! channel (pr-str response))))))

(defn handler
  [request]
  (async/as-channel
   request
   {:on-open connect!
    :on-close disconnect!
    :on-message handle-message!}))

(defn websocket-routes
  []
  ["/ws" {:get handler}])
