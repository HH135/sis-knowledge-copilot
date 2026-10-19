CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGSERIAL PRIMARY KEY,
    origin VARCHAR(20) NOT NULL,
    source_kind VARCHAR(40) NOT NULL,
    source_ref TEXT NOT NULL,
    title TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    content TEXT NOT NULL,
    content_sha256 CHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(title, '') || ' ' || left(coalesce(content, ''), 100000))
    ) STORED
);

ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS content_sha256 CHAR(64);
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS search_vector TSVECTOR
    GENERATED ALWAYS AS (
        to_tsvector('simple', coalesce(title, '') || ' ' || left(coalesce(content, ''), 100000))
    ) STORED;

CREATE INDEX IF NOT EXISTS ix_document_chunk_search
    ON document_chunk USING GIN (search_vector);
CREATE INDEX IF NOT EXISTS ix_document_chunk_origin_kind
    ON document_chunk (origin, source_kind);
CREATE UNIQUE INDEX IF NOT EXISTS ux_document_chunk_origin_kind_ref_line
    ON document_chunk (origin, source_kind, source_ref, start_line);

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
