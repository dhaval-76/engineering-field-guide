-- Posts, partitioned by author (Cassandra-style); bodies live here, feeds hold only ids.
CREATE TABLE posts_by_author (
    author_id  bigint,
    post_id    bigint,          -- Snowflake id: time-ordered
    body       text,
    media_keys list<text>,      -- objects in object storage, served via CDN
    PRIMARY KEY ((author_id), post_id)
) WITH CLUSTERING ORDER BY (post_id DESC);

-- Both directions of the follow graph
CREATE TABLE followers (user_id bigint, follower_id bigint, PRIMARY KEY ((user_id), follower_id));
CREATE TABLE followees (user_id bigint, followee_id bigint, is_celebrity boolean,
                        PRIMARY KEY ((user_id), followee_id));

-- Feed cache (Redis), one sorted set per active user:
--   ZADD   feed:{userId} <timestamp-or-score> <postId>
--   ZREMRANGEBYRANK feed:{userId} 0 -801        -- keep the newest 800
-- EXPIRE feed:{userId} 2592000                 -- 30 days idle → evicted, rebuilt on
-- return
