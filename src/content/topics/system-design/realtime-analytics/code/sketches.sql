-- ClickHouse: per-minute rollups that also keep mergeable unique-user sketches
CREATE TABLE page_minutes (
    minute     DateTime,
    page       LowCardinality(String),
    country    LowCardinality(String),
    views      UInt64,
    uniques    AggregateFunction(uniq, UInt64)      -- HyperLogLog-style state, mergeable
) ENGINE = AggregatingMergeTree
PARTITION BY toYYYYMM(minute)
ORDER BY (page, country, minute);

-- Daily unique visitors per page for the last 90 days: merges sketches, reads rollups
-- only
SELECT toDate(minute) AS day, page, sum(views) AS views, uniqMerge(uniques) AS visitors
FROM page_minutes
WHERE minute >= now() - INTERVAL 90 DAY
GROUP BY day, page
ORDER BY day, views DESC;
