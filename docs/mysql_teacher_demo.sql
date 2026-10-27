-- Picturehouse Jaipur: read-only inspection and reversible DML demo
-- Run this in MySQL Workbench while connected to your local MySQL server.
-- It assumes the project schema is named `picturehouse`.
-- The INSERT and UPDATE are rolled back at the end; no practice movie remains.

USE picturehouse;

-- 1) Confirm which server and schema you are connected to.
SELECT DATABASE() AS current_schema, VERSION() AS mysql_version;

-- 2) List all tables in the project schema.
SHOW TABLES;

-- 3) Inspect actual table definitions. SHOW CREATE TABLE includes columns,
-- primary keys, foreign keys, unique keys, indexes, and other table options.
SHOW CREATE TABLE movies;
SHOW CREATE TABLE theatres;
SHOW CREATE TABLE showtimes;
SHOW CREATE TABLE showtime_seats;
SHOW CREATE TABLE bookings;
SHOW CREATE TABLE ticket_bookings;
SHOW CREATE TABLE ride_posts;
SHOW CREATE TABLE ride_join_requests;
SHOW CREATE TABLE theatre_owner_memberships;

-- 4) List constraints and foreign-key relationships across the schema.
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;

SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME,
       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION;

-- 5) Inspect indexes (including unique indexes).
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 6) Browse current project data. These are read-only examples.
SELECT * FROM movies ORDER BY id;
SELECT * FROM theatres ORDER BY id;
SELECT * FROM showtimes ORDER BY id LIMIT 20;
SELECT * FROM showtime_seats ORDER BY id LIMIT 20;
SELECT * FROM ticket_bookings ORDER BY id DESC LIMIT 20;
SELECT * FROM ride_posts ORDER BY id DESC LIMIT 20;

-- 7) Demonstrate INSERT and UPDATE safely inside a transaction.
-- The selected columns match the movies table in this project.
START TRANSACTION;

INSERT INTO movies (title, language, genre, certification, duration_minutes,
                    synopsis, active)
VALUES ('SQL Demo - rollback me', 'Hindi', 'Demo', 'U', 1,
        'Temporary row for a classroom INSERT/UPDATE demonstration.', 1);

SET @demo_movie_id = LAST_INSERT_ID();

-- Show the newly inserted row and then update it.
SELECT * FROM movies WHERE id = @demo_movie_id;

UPDATE movies
SET title = 'SQL Demo - updated before rollback', duration_minutes = 2
WHERE id = @demo_movie_id;

SELECT * FROM movies WHERE id = @demo_movie_id;

-- Roll back the example INSERT and UPDATE so the database remains unchanged.
ROLLBACK;

-- This should return zero rows: the temporary record was rolled back.
SELECT * FROM movies WHERE title LIKE 'SQL Demo -%';

-- Notes for discussion:
-- * JPA/Hibernate creates/updates the schema from Java entity classes when
--   spring.jpa.hibernate.ddl-auto=update is active.
-- * Application INSERT/UPDATE statements are normally generated at runtime
--   by Spring Data JPA/Hibernate; they are not all stored in a hand-written
--   SQL file in this repository.
-- * Bean validation annotations such as @NotBlank are application-level
--   validation; inspect SHOW CREATE TABLE to see which rules are database-level.
