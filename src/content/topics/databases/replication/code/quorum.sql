-- Cassandra (CQL). Keyspace with N = 3 replicas per data centre.
CREATE KEYSPACE shop WITH replication = {'class': 'NetworkTopologyStrategy', 'eu-west': 3};

-- Consistency is chosen per request (cqlsh: CONSISTENCY; drivers: a statement option).
CONSISTENCY QUORUM;                 -- W = 2 of 3
INSERT INTO carts (user_id, item_id, qty) VALUES (?, ?, 2);

-- R = 2 of 3 → W + R = 4 > 3: overlaps the latest write
CONSISTENCY QUORUM;
SELECT * FROM carts WHERE user_id = ?;

-- Options for N = 3:
--   W=1, R=1   fastest, available with 2 nodes down, reads may be stale
--   W=2, R=2   QUORUM/QUORUM: read-your-writes, tolerates 1 node down
--   W=3, R=1   reads fast and fresh, but any single node down blocks writes
--   LOCAL_QUORUM: a quorum inside the local data centre — no cross-region latency
--
-- Lightweight transaction (Paxos) for compare-and-set, much slower:
INSERT INTO usernames (name, user_id) VALUES ('asha', ?) IF NOT EXISTS;
