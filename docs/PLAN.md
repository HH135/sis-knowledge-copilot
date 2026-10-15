# SIS App Intelligence - 4-Day MVP Plan

## Current State

- Java 21, Spring Boot 4.1.1, Maven, Thymeleaf home page (`/`) and its tests are in place.
- The current application packages are `io.github.hyun.sis_app_intelligence` and `controller`. Database integration, collectors, metadata adapters, RAG, and MCP are not implemented yet.
- The app currently uses Thymeleaf, Validation, and WebMVC dependencies. JPA and PostgreSQL dependencies were previously removed to keep the app web-only.
- The immediate implementation boundary is **Day 1, Task 1 only**: save this plan, add PostgreSQL/JDBC configuration and the initial schema, verify startup/tests, then stop. Do not begin any later task.

## MVP Goal and Security Modes

Build a four-day portfolio MVP that demonstrates SIS source and metadata search, cited RAG, procedure call relationships, retrieval evaluation, MCP tools, and a tool-calling agent.

The same application supports two profiles:

- **`internal`**: scan an externally configured SIS source path and read MSSQL catalog metadata using a read-only database account. Search only with PostgreSQL full-text search and optional `pg_trgm`. External LLM and embedding calls must be disabled.
- **`portfolio`**: use only committed fictional source, procedure, and table fixtures. Use Mock AI until an approved API is configured. Any real API adapter requires an explicit opt-in and may receive only portfolio data.

Never commit real SIS source/schema/documents, credentials, or actual internal paths. Exclude authentication, scheduling, production deployment, Ollama/local models, advanced parsing, and a polished UI from the MVP.

## Four-Day Sequence

| Day | Scope | Visible completion evidence |
|---|---|---|
| 1 | DB/schema, portfolio fixtures, metadata adapters/search API, source scanning, MSSQL catalog adapter | PostgreSQL-backed app starts; four initial tables exist; fixtures, APIs, file scan, and read-only metadata sync are verified in the order below. |
| 2 | `AiAnalysisPort`, Mock adapter, Java/JSP/procedure chunking, portfolio embeddings and vector storage | Chunks preserve source information and portfolio vectors can be stored/searched. Vector DDL is isolated in `schema-vector.sql`. Spring AI compatibility is verified by dependency resolution, compile, and context test before adoption. |
| 3 | Cited RAG, `EXEC`/`EXECUTE` relationship parsing, 20-question retrieval evaluation | Responses cite file/table/procedure sources; call relationships and repeatable Hit@k/Recall@k results are visible. |
| 4 | MCP tools, tool-calling demo, README and portfolio preparation | `search_columns`, `get_procedure_source`, and `get_call_tree` work through an MCP client. Mock is used until an approved API is available. |

### Fixed Day 1 Order

1. **Task 1 (only task in this implementation):** save this plan, add PostgreSQL/JDBC configuration and initial schema; verify app startup, four tables, existing home page, and tests.
2. **Task 2a:** define `tables.json` format, 20 fictional table/column specifications, and mark 3-5 intentional documentation discrepancies.
3. **Task 2b:** add 15-20 fictional `procedures/*.sql` fixtures.
4. **Task 2c:** add 30-50 fictional Java/JSP/XML source files.
5. **Task 4:** implement only `MetadataSourcePort` and `DummyMetadataSource`; no MSSQL connection/driver.
6. **Task 5:** implement column and procedure search APIs.
7. **Task 3:** implement source scanning, path confinement, hashing, and configurable charset handling.
8. **Task 4b:** implement the internal-profile-only MSSQL metadata source.

## Day 1 Task 1 - Database and Schema

### Scope and acceptance criteria

- Keep Java 21 and Spring Boot 4.1.1. Use Boot 4.x starters, APIs, and imports only; do not generate Boot 3 code.
- Add only the Spring Boot JDBC starter and PostgreSQL JDBC driver. Do not add JPA, Flyway, MSSQL JDBC, or Spring AI in this task.
- Use environment variables for PostgreSQL connection values; do not commit passwords. Add a local PostgreSQL 17 Compose service only if Docker is available.
- Load `src/main/resources/db/schema.sql` at startup.
- Schema creates only the `pg_trgm` extension and `source_file`, `meta_table`, `meta_column`, and `meta_proc`.
- Every `CREATE TABLE` and `CREATE INDEX` uses `IF NOT EXISTS`.
- Store source/procedure text as provided, but cap the generated full-text search vector input at the first 100,000 characters.
- Connect to PostgreSQL, start the application, verify the four tables, and pass the existing home-page and complete test suite.
- Store credentials only in environment variables or ignored local configuration.

### Expected files

- `docs/PLAN.md`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/resources/db/schema.sql`
- `.gitignore`
- `compose.yaml` only if needed and Docker is available
- Minimal task-1 schema/startup tests as needed

## Initial PostgreSQL Schema

This is the task-1 schema. `document_vector`, the pgvector extension, and `proc_dependency` are deliberately excluded.

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS source_file (
    id BIGSERIAL PRIMARY KEY,
    relative_path TEXT NOT NULL UNIQUE,
    file_type VARCHAR(30) NOT NULL,
    charset_name VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple',
            left(coalesce(relative_path, '') || ' ' || coalesce(content, ''), 100000))
    ) STORED
);
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
    definition_status VARCHAR(40) NOT NULL DEFAULT 'available',
    captured_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (database_name, schema_name, procedure_name),
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple',
            left(coalesce(procedure_name, '') || ' ' || coalesce(definition, ''), 100000))
    ) STORED
);
CREATE INDEX IF NOT EXISTS ix_meta_proc_search
    ON meta_proc USING GIN (search_vector);
CREATE INDEX IF NOT EXISTS ix_meta_proc_name_trgm
    ON meta_proc USING GIN (procedure_name gin_trgm_ops);
```

The vector extension and `document_vector` belong only in `src/main/resources/db/schema-vector.sql` on Day 2. `proc_dependency` belongs to the Day 3 relationship work. Confirm Spring AI PgVectorStore's required table/column format with a real compatibility test before using it.

## Day 1 Follow-up Design (not part of the current task)

- **2a fixtures:** `src/main/resources/portfolio-data/tables.json` defines the format and 20 fictional tables. Add 3-5 explicitly marked intentional discrepancies for later spec comparison.
- **2b procedures:** 15-20 fictional procedures under `src/main/resources/portfolio-data/procedures/`, with consistent names and some `EXEC` calls for later parsing.
- **2c source fixtures:** 30-50 fictional Java/JSP/XML files in the same fictional domain; no SIS-derived content.
- **Task 4:** `MetadataSourcePort` and `DummyMetadataSource` only. Do not add MSSQL dependencies or beans. Internal mode must fail explicitly until its real adapter is added.
- **Task 5 APIs:** `GET /api/metadata/columns?q=...` and `GET /api/metadata/procedures?q=...`, using parameterized SQL and tests.
- **Task 3 file scanning:** only `.java`, `.jsp`, `.xml`, `.sql`, `.properties`, `.ini`; external root configured outside the repo; path/symlink escape denied; source files are never modified. Preserve charset override with BOM detection, UTF-8 validation, and configurable MS949/EUC-KR fallback. Do not schedule scans.
- **Task 4b MSSQL:** allow SELECT only from `sys.schemas`, `sys.tables`, `sys.columns`, `sys.types`, `sys.procedures`, `sys.sql_modules`, and optional `sys.extended_properties`. A NULL procedure definition is persisted with status `definition 접근 불가`; do not invent source text. Read-only is enforced by the MSSQL account's server permissions, not by `Connection.setReadOnly`. Create a separate MSSQL `DataSource` and `JdbcTemplate` bean only under `internal`; never replace or alter the primary PostgreSQL datasource. Add `mssql-jdbc` only in task 4b. Test with mocks, not the company SQL Server.

## Spring AI Compatibility Notes

- Spring AI 2.0.0 is the stable line checked in Maven Central metadata. Its upstream POM sets Spring Boot 4.1.0 and Java 17. This is evidence for the Boot 4.1 line, but exact Boot 4.1.1 starter compatibility remains subject to the Day 2 dependency/compile/context test.
- Official PgVector documentation describes `spring-ai-starter-vector-store-pgvector`; verify the chosen version's schema expectations before committing vector DDL.
- Official Spring AI documentation includes MCP support. Verify the exact MCP starter with Boot 4.1.1 on Day 4.
- If Spring AI integration fails compatibility checks, use `AiAnalysisPort` with an approved API through Spring `RestClient`; consider a separate Maven module for MCP. Internal mode remains full-text only. Do not use a local model.

References: [Spring AI Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html), [Spring AI 2.0.0 POM](https://github.com/spring-projects/spring-ai/blob/v2.0.0/pom.xml), [Maven Central BOM metadata](https://repo.maven.apache.org/maven2/org/springframework/ai/spring-ai-bom/maven-metadata.xml), [PgVector](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html), [MCP overview](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html).

## Remaining Days

### Day 2

Create `AiAnalysisPort` and `MockAiAnalysisAdapter`; chunk Java/JSP by source unit and procedures by procedure block while preserving citation metadata. Use pgvector only for portfolio data, with DDL isolated in `schema-vector.sql`. Test Spring AI dependencies against Boot 4.1.1 before adopting the starter. Until an approved provider and credential exist, do not claim real semantic embeddings; preserve the Mock/port seam.

### Day 3

Implement `RagQueryService` with source citations. Internal uses PostgreSQL lexical search and no model call; portfolio uses vector retrieval and Mock or an approved API. Add a bounded `EXEC`/`EXECUTE` parser and call-tree query. Add a versioned 20-question fixture with expected sources and a reproducible Hit@k/Recall@k report.

### Day 4

Expose only `search_columns`, `get_procedure_source`, and `get_call_tree` as MCP tools. Validate tool inputs and prohibit arbitrary SQL/filesystem access. Demonstrate tool calling with Mock until approved API access is available. Finish README and demo scenario.

### Documentation/Portfolio

- DOCX discrepancy MVP: manually normalized CSV/JSON spec compared with collected table/column metadata; report missing/extra table/column and type/description mismatch. Avoid Apache POI and automatic DOCX parsing in the critical path.
- README sections: project, modes/security boundary, architecture, versions/compatibility status, local DB setup, profiles/secrets, dummy fixtures, endpoints/curl, RAG evaluation, MCP demo, limitations.
- Video: portfolio search, cited RAG result, call tree, three MCP tools, and evidence that internal mode makes no external AI calls. Do not show real sources or secrets.
- Interview topics: data isolation, chunk provenance, FTS versus vector retrieval, AI port replacement, MCP tool input/permission limits.

## Task 1 Verification Commands

```powershell
docker compose up -d postgres
.\mvnw.cmd test
$env:SPRING_PROFILES_ACTIVE = "portfolio"
.\mvnw.cmd spring-boot:run
curl.exe http://localhost:8080/
```

Verify the database directly for `source_file`, `meta_table`, `meta_column`, and `meta_proc`. If Docker is unavailable, use an existing PostgreSQL 17 instance through environment variables. Do not report success if PostgreSQL connection, schema creation, app startup, or existing tests fail. Never print credentials or actual source paths in logs or screenshots.
