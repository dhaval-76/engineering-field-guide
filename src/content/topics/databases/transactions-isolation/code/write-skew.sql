-- Rule: at least one doctor must be on call for each shift.
CREATE TABLE on_call (doctor text, shift_id int, on_call boolean, PRIMARY KEY (doctor, shift_id));

-- Both Alice and Bob run this at the same time, each for themselves (Repeatable Read):
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT count(*) FROM on_call WHERE shift_id = 1 AND on_call;     -- both see 2
-- Bob: 'bob'
UPDATE on_call SET on_call = false WHERE shift_id = 1 AND doctor = 'alice';
-- both succeed → nobody on call
COMMIT;

-- Fix 1: SERIALIZABLE. One of the two commits fails with 40001 and is retried;
--        on retry it sees count = 1 and refuses.
BEGIN ISOLATION LEVEL SERIALIZABLE;
SELECT count(*) FROM on_call WHERE shift_id = 1 AND on_call;
UPDATE on_call SET on_call = false WHERE shift_id = 1 AND doctor = 'alice';
COMMIT;

-- Fix 2: lock the rows the decision is based on (works at Read Committed too).
BEGIN;
SELECT doctor FROM on_call WHERE shift_id = 1 AND on_call FOR UPDATE;   -- Bob waits
-- count the returned rows in the app; proceed only if > 1
UPDATE on_call SET on_call = false WHERE shift_id = 1 AND doctor = 'alice';
COMMIT;

-- Fix 3: materialize the conflict. Bookings can't overlap: the database enforces it,
-- even for rows that don't exist yet (which FOR UPDATE can't lock).
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE TABLE room_bookings (
    room_id int NOT NULL,
    during  tstzrange NOT NULL,
    -- no two rows: same room AND overlapping
    EXCLUDE USING gist (room_id WITH =, during WITH &&)
);
