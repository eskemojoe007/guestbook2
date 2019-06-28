(ns guestbook2.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[guestbook2 started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[guestbook2 has shut down successfully]=-"))
   :middleware identity})
