(ns guestbook2.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ajax.core :refer [GET POST]]
   [clojure.string :as string]
   [guestbook2.validation :refer [validate-message]]
   [guestbook2.websockets :as ws]
   [mount.core :as mount]))


; (-> (.getElementById js/document "content")
;     (.-innerHTML)
;     (set! "Hello, World!"))
; (r/render
;  [:div#hello.content>h1 "Hello, Auto!"]
;  (.getElementById js/document "content"))

;;;; re-frame functions

;; Init
(rf/reg-event-fx
 :app/initialize
 (fn [_ _]
   {:db {:messages/loading? true}
         ; :form/fields {:name ""
         ;               :message ""}}
    :dispatch [:messages/load]}))

;; Mess with messages
(rf/reg-event-db
 :messages/set
 (fn [db [_ messages]]
   (-> db
       (assoc :messages/loading? false
              :messages/list messages))))

(rf/reg-event-db
 :message/add
 (fn [db [_ message]]
   (update db :messages/list conj message)))

(rf/reg-sub
 :messages/loading?
 (fn [db _]
   (:messages/loading? db)))

(rf/reg-sub
 :messages/list
 (fn [db _]
   (:messages/list db [])))


;;; Fields basics
(rf/reg-event-db
 :form/set-field
 (fn [db [_ id value]]
   (assoc-in db [:form/fields id] value)))

(rf/reg-event-db
 :form/clear-fields
 (fn [db _]
   (assoc db :form/fields {})))

(rf/reg-sub
 :form/fields
 (fn [db _]
   (:form/fields db)))

(rf/reg-sub
 :form/field
 :<- [:form/fields]
 (fn [fields [_ id]]
   (get fields id)))

;; Errors
(rf/reg-event-db
 :form/set-server-errors
 (fn [db [_ errors]]
   (assoc db :form/server-errors errors)))

(rf/reg-sub
 :form/server-errors
 (fn [db _]
   (:form/server-errors db)))

; Use the function from fields to get validation errors
(rf/reg-sub
 :form/validation-errors
 :<- [:form/fields]
 (fn [fields _]
   (validate-message fields)))

; See if any errors exist
(rf/reg-sub
 :form/validation-errors?
 :<- [:form/validation-errors]
 (fn [errors _]
   (not (empty? errors))))

; All errors
(rf/reg-sub
 :form/errors
 :<- [:form/server-errors]
 :<- [:form/validation-errors]
 (fn [[server validation] _]
   (merge validation server)))

; Get just errors for a specific ID of form
(rf/reg-sub
 :form/error
 :<- [:form/errors]
 (fn [errors [_ id]]
   (get errors id)))

;; sending messages as an event instead of a trad function.
(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ fields]]
   (ws/send!
    [:message/create! fields] ; Send the create!
    10000 ; set the timeout to 10 seconds

    ; Set the call back function to complete when
    (fn [{:keys [success errors] :as response}]
      (.log js/console "called back: " (pr-str response))
      (if success
        (rf/dispatch [:form/clear-fields])
        (rf/dispatch [:form/set-server-errors errors]))))

   ; Set server errors to 0, the dispatch above will set them async if they exist
   {:db (dissoc db :form/server-errors)}))

(rf/reg-event-fx
 :messages/load
 (fn [{:keys [db]} _]
   (GET "/api/messages"
        {:headers {"Accept" "application/transit+json"}
         :handler #(rf/dispatch [:messages/set (:messages %)])})
   ; Similar to above, set loading, :message/set will change to false when done.
   {:db (assoc db :messages/loading? true)}))


(defn errors-component
  "React component based on a :form/error.
  id - the key used to extract the kind of error we have"
  [id]
  (when-let [error @(rf/subscribe [:form/error id])]
    [:div.notification.is-danger (string/join error)]))

(defn message-form
  "Component that is a message form"
  []
  (fn []
    [:div
     [errors-component :server-error]
     [:div.field
      [:label.label {:for :name} "Name"]
      [errors-component :name]
      [:input.input
       {:type :text
        :name :name
        :on-change #(rf/dispatch [:form/set-field :name (-> % .-target .-value)])
        :value @(rf/subscribe [:form/field :name])}]]
     [:div.field
      [:label.label {:for :message} "Message"]
      [errors-component :message]
      [:textarea.textarea
       {:type :text
        :name :message
        :on-change #(rf/dispatch [:form/set-field :message (-> % .-target .-value)])
        :value @(rf/subscribe [:form/field :message])}]]
     [:input.button.is-primary
      {:type :submit
       ; :on-click #(send-message! (rf/subscribe [:form/fields]) errors)
       :on-click #(rf/dispatch [:message/send! @(rf/subscribe [:form/fields])])
       :disabled @(rf/subscribe [:form/validation-errors?])
       :value "comment"}]
     [:p "Name: " @(rf/subscribe [:form/field :name])]
     [:p "Message: " @(rf/subscribe [:form/field :message])]]))

(defn message-list
  "Sub Reagent component to display message lists."
  [messages]
  [:ul.messages
   (for [{:keys [timestamp message name]} (reverse @messages)]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p "@" name]])])

(defn home
  "Reagent component of the main home page."
  []
  (let [messages (rf/subscribe [:messages/list])]
    (fn []
      (if @(rf/subscribe [:messages/loading?])
        [:div>div.row>div.span12>h3 "Loading Messages..."]
        [:div.content
         [:div.columns.is-centered>div.column.is-two-thirds
          [:div.columns>div.column
           [message-form]]
          [:div.columns>div.column
           [:h3 "Messages"]
           [message-list messages]]]]))))

(defn mount-components
  "Mounts reagent components to be rendered"
  []
  (.log js.console "Mounting Components...")
  (r/render [#'home] (.getElementById js/document "app"))
  (.log js.console "Components Mounted!"))

(defn init!
  "Initializes a connection and front end."
  []
  (.log js.console "Initializing App..")
  (mount/start)
  (rf/dispatch [:app/initialize])
  (mount-components))
