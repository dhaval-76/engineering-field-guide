CREATE TABLE blobs (
    hash        bytea PRIMARY KEY,          -- sha256(content)
    kind        text NOT NULL,              -- 'full' or 'delta'
    base_hash   bytea REFERENCES blobs(hash),   -- for deltas: the blob it applies to
    chain_len   int NOT NULL DEFAULT 0,     -- deltas since the last full snapshot
    data        bytea NOT NULL              -- compressed content or delta (or an object-storage key)
);

CREATE TABLE versions (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    page_id     bigint NOT NULL,
    parent_id   bigint REFERENCES versions(id),
    merge_parent_id bigint REFERENCES versions(id),   -- second parent for merges
    content_hash bytea NOT NULL REFERENCES blobs(hash),
    author_id   bigint NOT NULL,
    message     text,
    created_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX versions_page_history ON versions (page_id, id DESC);

CREATE TABLE pages (
    id          bigint PRIMARY KEY,
    head_id     bigint NOT NULL REFERENCES versions(id)
);

-- Save = new version + move head only if nobody else moved it (compare-and-set)
-- UPDATE pages SET head_id = :new WHERE id = :page AND head_id = :expectedHead;  -- 0 rows → conflict
