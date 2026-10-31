-- PostgreSQL + pgvector: chunks with metadata, a vector index and a keyword index.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE doc_chunk (
  id          bigserial PRIMARY KEY,
  tenant_id   bigint      NOT NULL,
  doc_id      text        NOT NULL,
  chunk_no    int         NOT NULL,
  title       text        NOT NULL,
  content     text        NOT NULL,
  acl_groups  text[]      NOT NULL,              -- who may see it, copied from the source
  embedding   vector(1024) NOT NULL,
  embed_model text        NOT NULL,              -- re-embed everything when this changes
  updated_at  timestamptz NOT NULL DEFAULT now(),
  tsv         tsvector GENERATED ALWAYS AS (to_tsvector('english', title || ' ' || content)) STORED,
  UNIQUE (doc_id, chunk_no)
);

-- HNSW: approximate nearest neighbour; cosine distance operator is <=>
CREATE INDEX doc_chunk_embedding_hnsw ON doc_chunk
  USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
CREATE INDEX doc_chunk_tsv ON doc_chunk USING gin (tsv);
CREATE INDEX doc_chunk_tenant ON doc_chunk (tenant_id);

-- Vector search (recall/latency dial: SET hnsw.ef_search = 100;)
SELECT id, doc_id, content, 1 - (embedding <=> $1) AS similarity
FROM doc_chunk
-- user's groups overlap the chunk's ACL
WHERE tenant_id = $2 AND acl_groups && $3
ORDER BY embedding <=> $1
LIMIT 50;

-- Keyword search (BM25-like ranking)
SELECT id, doc_id, content, ts_rank(tsv, q) AS rank
FROM doc_chunk, websearch_to_tsquery('english', $1) q
WHERE tenant_id = $2 AND acl_groups && $3 AND tsv @@ q
ORDER BY rank DESC
LIMIT 50;
