(ns guestbook2.messages
  (:require
   [guestbook2.db.core :as db]
   [guestbook2.validation :refer [validate-message]]))

(defn message-list
  "Returns a vector of message using the database `get-messages` query."
  []
  {:messages (vec (db/get-messages))})

(defn save-message!
  "Saves a new message to the main db."
  [message]
  (if-let [errors (validate-message message)]
    (throw (ex-info "Message is invalid"
                    {:guestbook2/error-id :validation
                     :errors errors}))
    (db/save-message! message)))
