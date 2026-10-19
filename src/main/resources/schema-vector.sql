CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGSERIAL PRIMARY KEY,
    origin VARCHAR(20) NOT NULL,
    source_kind VARCHAR(40) NOT NULL,
    source_ref TEXT NOT NULL,
    title TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    content TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS chunk_embedding (
    chunk_id BIGINT PRIMARY KEY REFERENCES document_chunk(id) ON DELETE CASCADE,
    model VARCHAR(50) NOT NULL,
    dimensions INT NOT NULL,
    content_sha256 CHAR(64) NOT NULL,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_chunk_embedding_hnsw_cosine
    ON chunk_embedding USING hnsw (embedding vector_cosine_ops);
