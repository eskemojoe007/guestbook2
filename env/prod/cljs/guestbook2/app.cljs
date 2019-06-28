(ns guestbook2.app
  (:require [guestbook2.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
