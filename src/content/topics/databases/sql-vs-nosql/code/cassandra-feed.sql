-- CQL. Query to serve: "latest activity for user X, newest first, 20 at a time".
CREATE TABLE activity_by_user (
    user_id     uuid,
    month       text,          -- bucket: keeps one user's partition bounded ('2026-03')
    created_at  timeuuid,
    actor_id    uuid,
    verb        text,          -- 'liked', 'commented', …
    object_id   uuid,
    summary     text,          -- denormalized: everything the screen shows, no joins
    PRIMARY KEY ((user_id, month), created_at)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- One partition, already sorted → a single sequential read.
SELECT * FROM activity_by_user
WHERE user_id = ? AND month = '2026-03'
LIMIT 20;

-- Another query ("activity on object Y") is ANOTHER table, written alongside this one:
CREATE TABLE activity_by_object (
    object_id   uuid,
    created_at  timeuuid,
    user_id     uuid,
    verb        text,
    PRIMARY KEY ((object_id), created_at)
) WITH CLUSTERING ORDER BY (created_at DESC);

-- ✗ SELECT * FROM activity_by_user WHERE verb = 'liked';
--   → rejected without ALLOW FILTERING: it would scan every partition in the cluster.
