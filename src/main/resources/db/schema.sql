CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS source_file (
    id BIGSERIAL PRIMARY KEY,
    relative_path TEXT NOT NULL,
    file_type VARCHAR(30) NOT NULL,
    charset_name VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    origin VARCHAR(20) NOT NULL DEFAULT 'portfolio',
    size_bytes BIGINT,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector(
            'simple',
            left(coalesce(relative_path, '') || ' ' || coalesce(content, ''), 100000)
        )
    ) STORED
);

ALTER TABLE source_file ADD COLUMN IF NOT EXISTS origin VARCHAR(20) NOT NULL DEFAULT 'portfolio';
ALTER TABLE source_file ADD COLUMN IF NOT EXISTS size_bytes BIGINT;
ALTER TABLE source_file DROP CONSTRAINT IF EXISTS source_file_relative_path_key;

CREATE UNIQUE INDEX IF NOT EXISTS ux_source_file_origin_relative_path
    ON source_file (origin, relative_path);

CREATE INDEX IF NOT EXISTS ix_source_file_search
    ON source_file USING GIN (search_vector);
CREATE INDEX IF NOT EXISTS ix_source_file_path_trgm
    ON source_file USING GIN (relative_path gin_trgm_ops);

CREATE TABLE IF NOT EXISTS meta_table (
    id BIGSERIAL PRIMARY KEY,
    database_name TEXT NOT NULL,
    schema_name TEXT NOT NULL,
    table_name TEXT NOT NULL,
    description TEXT,
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (database_name, schema_name, table_name)
);

CREATE INDEX IF NOT EXISTS ix_meta_table_name_trgm
    ON meta_table USING GIN (table_name gin_trgm_ops);

CREATE TABLE IF NOT EXISTS meta_column (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT NOT NULL REFERENCES meta_table(id) ON DELETE CASCADE,
    column_name TEXT NOT NULL,
    ordinal_position INTEGER,
    data_type TEXT,
    is_nullable BOOLEAN,
    description TEXT,
    UNIQUE (table_id, column_name)
);

CREATE INDEX IF NOT EXISTS ix_meta_column_name_trgm
    ON meta_column USING GIN (column_name gin_trgm_ops);

CREATE TABLE IF NOT EXISTS meta_proc (
    id BIGSERIAL PRIMARY KEY,
    database_name TEXT NOT NULL,
    schema_name TEXT NOT NULL,
    procedure_name TEXT NOT NULL,
    definition TEXT,
    definition_status VARCHAR(20) NOT NULL DEFAULT 'OK',
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (database_name, schema_name, procedure_name),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector(
            'simple',
            left(coalesce(procedure_name, '') || ' ' || coalesce(definition, ''), 100000)
        )
    ) STORED
);

CREATE INDEX IF NOT EXISTS ix_meta_proc_search
    ON meta_proc USING GIN (search_vector);
CREATE INDEX IF NOT EXISTS ix_meta_proc_name_trgm
    ON meta_proc USING GIN (procedure_name gin_trgm_ops);

ALTER TABLE meta_proc ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE meta_proc ADD COLUMN IF NOT EXISTS source_ref TEXT;
ALTER TABLE meta_proc ADD COLUMN IF NOT EXISTS definition_status VARCHAR(20) NOT NULL DEFAULT 'OK';

CREATE TABLE IF NOT EXISTS proc_dependency (
    id BIGSERIAL PRIMARY KEY,
    origin VARCHAR(20) NOT NULL,
    caller_type VARCHAR(20) NOT NULL,
    caller_ref TEXT NOT NULL,
    callee_schema VARCHAR(128) NOT NULL DEFAULT 'dbo',
    callee_name VARCHAR(256) NOT NULL,
    call_type VARCHAR(20) NOT NULL,
    line_no INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE NULLS NOT DISTINCT
        (origin, caller_type, caller_ref, callee_schema, callee_name, line_no)
);

CREATE INDEX IF NOT EXISTS ix_proc_dependency_origin_callee
    ON proc_dependency (origin, callee_name);
CREATE INDEX IF NOT EXISTS ix_proc_dependency_origin_caller
    ON proc_dependency (origin, caller_ref);
