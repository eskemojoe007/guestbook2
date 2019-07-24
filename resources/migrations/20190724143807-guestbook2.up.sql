CREATE TABLE guestbook2
(id INTEGER PRIMARY KEY AUTO_INCREMENT,
name VARCHAR(30),
message VARCHAR(200),
-- By setting a default timestamp, we don't always need to provide on
-- in the clojure code that calls the migration
timestamp TIMESTAMP default CURRENT_TIMESTAMP);
