(ns guestbook2.validation
  (:require [struct.core :as st]))

;; TODO: Write unit test...not sure how to do that for CLJC files
;; noisesmith says a single test can be written then both executed
;; in each of the clj and cljs sections.  I will have to look into that.
;; make a test file for my cljc tests, then create standard
;; functions (defn) that call test/is and/or other
;; test functions I need.
;; Then make simple wrapper deftests in each clj and cljs files.  
(def message-schema
  [[:name
    st/required
    st/string
    {:message "name cannot be longer than 30 characters"
     :validate #(<= (count %) 30)}]
   [:message
    st/required
    st/string
    {:message "message must contain at least 10 characters"
     :validate #(>= (count %) 10)}
    {:message "message cannot be longer than 200 characters"
     :validate #(<= (count %) 200)}]])

(defn validate-message
  [params]
  (first (st/validate params message-schema)))
