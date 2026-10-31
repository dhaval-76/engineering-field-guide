-- Cassandra / ScyllaDB (CQL)
CREATE TABLE messages (
    conversation_id uuid,
    -- e.g. month number: bounds very active conversations' partitions
    bucket          int,
    seq             bigint,           -- per-conversation order, assigned by the server
    message_id      uuid,             -- server id
    client_msg_id   text,             -- sender's id, for de-duplication
    sender_id       uuid,
    body            blob,             -- ciphertext when end-to-end encrypted
    sent_at         timestamp,
    PRIMARY KEY ((conversation_id, bucket), seq)
) WITH CLUSTERING ORDER BY (seq DESC);

-- Latest page
SELECT * FROM messages WHERE conversation_id = ? AND bucket = ? LIMIT 50;
-- Catch-up after reconnect
SELECT * FROM messages WHERE conversation_id = ? AND bucket = ? AND seq > ? ORDER BY seq ASC;

-- Per member: how far each one has read (one row per member, not one per message)
CREATE TABLE read_positions (
    conversation_id uuid,
    user_id         uuid,
    read_seq        bigint,
    delivered_seq   bigint,
    PRIMARY KEY ((conversation_id), user_id)
);

-- A user's conversations, most recent first (the inbox)
CREATE TABLE inbox (
    user_id          uuid,
    last_activity    timestamp,
    conversation_id  uuid,
    preview          text,
    PRIMARY KEY ((user_id), last_activity, conversation_id)
) WITH CLUSTERING ORDER BY (last_activity DESC, conversation_id ASC);
