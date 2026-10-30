-- PostgreSQL: let the database forbid overlaps, no matter how many app servers there are.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
    id         uuid PRIMARY KEY,
    room_id    text        NOT NULL,
    during     tstzrange   NOT NULL,          -- [start, end) by default
    organizer  text        NOT NULL,
    series_id  uuid,
    EXCLUDE USING gist (room_id WITH =, during WITH &&)   -- same room AND overlapping range → rejected
);

-- Two concurrent inserts for overlapping times: one succeeds, the other fails with
-- SQLSTATE 23P01 (exclusion_violation) → return 409 Conflict and suggest another room.
INSERT INTO bookings (id, room_id, during, organizer)
VALUES (gen_random_uuid(), 'R-301', tstzrange('2026-11-02 10:00+05:30', '2026-11-02 11:00+05:30'), 'asha');

-- Without exclusion constraints (e.g. MySQL): lock the room row, then check, then insert, in one transaction.
-- SELECT id FROM rooms WHERE id = 'R-301' FOR UPDATE;
-- SELECT 1 FROM bookings WHERE room_id = 'R-301' AND start_at < :end AND end_at > :start;
-- INSERT … ;  COMMIT;
