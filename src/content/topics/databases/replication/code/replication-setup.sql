-- primary: postgresql.conf
--   wal_level = replica
--   max_wal_senders = 10
-- synchronous_standby_names = 'FIRST 1 (standby_a, standby_b)'   -- wait for one of these
-- synchronous_commit = on        -- 'remote_apply' also waits until the standby has
-- APPLIED it
--
-- A transaction can relax durability when losing it is acceptable (e.g. analytics
-- events):
SET LOCAL synchronous_commit = local;

-- On the primary: who is connected, sync or async, and how far behind
SELECT application_name, sync_state, state,
       pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn)) AS replay_lag_bytes,
       write_lag, flush_lag, replay_lag
FROM pg_stat_replication;

-- On a replica: how old is the data I'm serving?
SELECT now() - pg_last_xact_replay_timestamp() AS replay_delay,
       pg_last_wal_replay_lsn()             AS replayed_up_to;

-- Read-your-writes by position: after a write, the primary reports its WAL position…
SELECT pg_current_wal_lsn();                              -- e.g. 3/A2001F28
-- …and a replica may serve that user only once it has replayed at least that far:
SELECT pg_last_wal_replay_lsn() >= '3/A2001F28'::pg_lsn AS caught_up;
