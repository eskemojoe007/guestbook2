(ns guestbook2.websockets
  (:require [cljs.reader :as edn]))

(defonce channel (atom nil))

(defn connect!
  [url receive-handler]
  (if-let [chan (js/WebSocket. url)]
    (do
      (.log js/console "Connected!")
      (set! (.-onmessage chan) #(->> %
                                     .-data
                                     edn/read-string
                                     receive-handler)))
    (throw (ex-info "Websocke connection failed!" {:url url}))))

(defn send-message!
  [msg]
  (if-let [chan @channel]
    (.send chan (pr-str msg))
    (throw (ex-info "Couldn't sent message, channel isn't open" {:message msg}))))
