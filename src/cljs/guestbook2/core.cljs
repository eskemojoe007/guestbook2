(ns guestbook2.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [ajax.core :refer [GET POST]]
   [clojure.string :as string]
   [guestbook2.validation :refer [validate-message]]
   [guestbook2.websockets :as ws]))


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


;; Fields
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
   (ws/send-message! fields)
   ; (POST "/api/message"
   ;       {:format :json
   ;        :headers {"Accept" "application/json"
   ;                  "x-csrf-token" (.-value (.getElementById js/document "token"))}
   ;        :params fields
   ;        :handler #(rf/dispatch [:message/add (-> fields
   ;                                                 (assoc :timestamp (js/Date.)))])
   ;        :error-handler #(rf/dispatch [:form/set-server-errors
   ;                                      (get-in % [:response :errors])])})
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

(defn handle-response!
  [response]
  (if-let [errors (:errors response)]
    (rf/dispatch [:form/set-server-errors errors])
    (do
      (rf/dispatch [:message/add response])
      (rf/dispatch [:form/clear-fields response]))))

;;;; Reagent Functions
; (defn get-messages
;   "Gets messages from api and dispatches messages"
;   []
;   (GET "/api/messages"
;        {:headers {"Accept" "application/transit+json"}
;         :handler #(do
;                     ; (.log js/console (str (:messages %)))
;                     (rf/dispatch [:messages/set (:messages %)]))}))

(defn errors-component
  "React component based on an errors atom.
  errors - r/atom that is used to store our errors.
  id - the key used to extract the kind of error we have"
  [id]
  (when-let [error @(rf/subscribe [:form/error id])]
    [:div.notification.is-danger (string/join error)]))

; (defn message-list-component
;   [message]
;   [:li
;    [:time (:timestamp message)]
;    [:p (:message message)]
;    [:p (str " - " (:name message))]])

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
  [messages]
  [:ul.messages
   ; (for [{:keys [timestamp message name]} (sort-by :timestamp #(compare %2 %1) @messages)])
   (for [{:keys [timestamp message name]} (reverse @messages)]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p "@" name]])])

(defn home []
  (let [messages (rf/subscribe [:messages/list])]
    ; (rf/dispatch [:app/initialize])
    ; (get-messages)
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
  []
  (.log js.console "Mounting Components...")
  (r/render [#'home] (.getElementById js/document "app"))
  (.log js.console "Components Mounted!"))

(defn init!
  []
  (.log js.console "Initializing App..")
  (rf/dispatch [:app/initialize])
  (ws/connect! (str "ws://" (.-host js/location) "/ws") handle-response!)
  ; (get-messages)
  (mount-components))
; (.log js/console "guestbook.core evaluated!")
; (.log js/console "guestbook.core evaluated 2!")
;
; (r/render
;   [home]
;   (.getElementById js/document "content"))

; (ns guestbook2.core
;   (:require
;     [day8.re-frame.http-fx]
;     [reagent.core :as r]
;     [re-frame.core :as rf]
;     [goog.events :as events]
;     [goog.history.EventType :as HistoryEventType]
;     [markdown.core :refer [md->html]]
;     [guestbook2.ajax :as ajax]
;     [guestbook2.events]
;     ; [ajax.core :refer [GET POST]]
;     [reitit.core :as reitit]
;     [clojure.string :as string]
;     [guestbook2.validation :refer [validate-message]])
;   (:import goog.History))
;
;
;
; (defn nav-link [uri title page]
;   [:a.navbar-item
;    {:href   uri
;     :class (when (= page @(rf/subscribe [:page])) :is-active)}
;    title])
;
; (defn navbar []
;   (r/with-let [expanded? (r/atom false)]
;     [:nav.navbar.is-info>div.container
;      [:div.navbar-brand
;       [:a.navbar-item {:href "/" :style {:font-weight :bold}} "guestbook2"]
;       [:span.navbar-burger.burger
;        {:data-target :nav-menu
;         :on-click #(swap! expanded? not)
;         :class (when @expanded? :is-active)}
;        [:span][:span][:span]]]
;      [:div#nav-menu.navbar-menu
;       {:class (when @expanded? :is-active)}
;       [:div.navbar-start
;        [nav-link "#/" "Home" :home]
;        [nav-link "#/about" "About" :about]]]]))
;
; (defn about-page []
;   [:section.section>div.container>div.content
;    [:img {:src "/img/warning_clojure.png"}]])
;
; (defn home-page []
;   [:section.section>div.container>div.content
;    (when-let [docs @(rf/subscribe [:docs])]
;      [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])
;
; (def pages
;   {:home #'home-page
;    :about #'about-page})
;
; (defn page []
;   [:div
;    [navbar]
;    [(pages @(rf/subscribe [:page]))]])
;
; ;; -------------------------
; ;; Routes
;
; (def router
;   (reitit/router
;     [["/" :home]
;      ["/about" :about]]))
;
; ;; -------------------------
; ;; History
; ;; must be called after routes have been defined
; (defn hook-browser-navigation! []
;   (doto (History.)
;     (events/listen
;       HistoryEventType/NAVIGATE
;       (fn [event]
;         (let [uri (or (not-empty (string/replace (.-token event) #"^.*#" "")) "/")]
;           (rf/dispatch
;             [:navigate (reitit/match-by-path router uri)]))))
;     (.setEnabled true)))
;
; ;; -------------------------
; ;; Initialize app
; (defn mount-components []
;   (rf/clear-subscription-cache!)
;   (r/render [#'page] (.getElementById js/document "app")))
;
; (defn init! []
;   (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])
;
;   (ajax/load-interceptors!)
;   (rf/dispatch [:fetch-docs])
;   (hook-browser-navigation!)
;   (mount-components))
