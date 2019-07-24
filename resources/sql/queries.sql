-- :name save-message! :! :n
-- :doc creates a new message using the name and message keys
INSERT INTO guestbook2
(name, message)
VALUES (:name, :message)

-- :name get-messages :? :*
-- :doc retrieves all message records
SELECT * FROM guestbook2
