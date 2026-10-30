-- One row per (show, seat). Holding seats is a conditional UPDATE: it either changes every
-- requested row or the transaction is rolled back.
BEGIN;

UPDATE show_seats
   SET status = 'HELD', hold_id = :holdId, hold_expires_at = now() + interval '8 minutes'
 WHERE show_id = :showId
   AND seat_id = ANY(:seatIds)
   AND (status = 'AVAILABLE' OR (status = 'HELD' AND hold_expires_at < now()));

-- If the affected row count is less than the number of seats requested, someone else got at
-- least one seat: ROLLBACK and tell the user which seats are gone. Otherwise COMMIT.
COMMIT;

-- Confirm after successful payment (only if our hold is still valid):
UPDATE show_seats SET status = 'BOOKED'
 WHERE show_id = :showId AND hold_id = :holdId AND status = 'HELD' AND hold_expires_at >= now();

-- Housekeeping (optional, since holds also expire lazily in the WHERE clause above):
UPDATE show_seats SET status = 'AVAILABLE', hold_id = NULL
 WHERE status = 'HELD' AND hold_expires_at < now();
