-- Metadata lives in a relational DB (sharded by namespace/owner); bytes live in object
-- storage.
CREATE TABLE chunk (
  -- SHA-256 of content: stored once, shared by everyone
  hash      char(64) PRIMARY KEY,
  size      int      NOT NULL,
  -- garbage-collect at 0 (after a grace period)
  ref_count bigint   NOT NULL DEFAULT 0
);

CREATE TABLE file (
  id              bigserial PRIMARY KEY,
  namespace_id    bigint NOT NULL,           -- a user's root or a shared folder
  path            text   NOT NULL,
  current_version int    NOT NULL DEFAULT 0,
  deleted         boolean NOT NULL DEFAULT false,
  UNIQUE (namespace_id, path)
);

CREATE TABLE file_version (
  file_id    bigint NOT NULL REFERENCES file(id),
  version    int    NOT NULL,
  chunks     char(64)[] NOT NULL,            -- ordered list of chunk hashes
  size       bigint NOT NULL,
  device_id  text   NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (file_id, version)
);

-- Journal of changes per namespace: clients sync by asking "what changed after cursor N?"
CREATE TABLE change_journal (
  namespace_id bigint NOT NULL,
  seq          bigserial,
  file_id      bigint NOT NULL,
  version      int    NOT NULL,
  PRIMARY KEY (namespace_id, seq)
);

-- 1. Upload: which of these chunks does the server not have yet?  (client uploads only
-- those)
SELECT h FROM unnest($1::char(64)[]) AS h WHERE NOT EXISTS (SELECT 1 FROM chunk WHERE hash = h);

-- 2. Commit a new version, but only if the client edited the version the server still
-- has.
--    0 rows updated → someone else changed the file first → save as a "conflicted copy".
UPDATE file SET current_version = current_version + 1
WHERE id = $file_id AND current_version = $base_version
RETURNING current_version;

-- 3. Sync: everything after my cursor
SELECT seq, file_id, version FROM change_journal
WHERE namespace_id = $ns AND seq > $cursor ORDER BY seq LIMIT 1000;
