(ns guestbook2.test.db.core
  (:require
    [guestbook2.db.core :refer [*db*] :as db]
    [luminus-migrations.core :as migrations]
    [clojure.test :refer [deftest is are use-fixtures testing]]
    [clojure.java.jdbc :as jdbc]
    [guestbook2.config :refer [env]]
    [mount.core :as mount]
    [java-time :as time]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'guestbook2.config/env
      #'guestbook2.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-message
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (let [name "bob"
          message "Hello, world.  How are you?"
          timestamp (time/local-date-time)]
      (testing "Testing database messages:"
        (testing "Save new message"
          (is (= 1 (db/save-message!
                     t-conn
                     {:name name
                      :message message}))))
        (testing "- pull messages test values"
          (is (= {:name name
                  :message message}
                 (-> (db/get-messages t-conn {})
                     (first)
                     (select-keys [:name :message]))))
          (testing "- timestamp is before now"
            (is (>= 0 (compare
                        timestamp
                        (-> (db/get-messages t-conn {})
                            (first)
                            (:timestamp)))))))))))
