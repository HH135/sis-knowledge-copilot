# SIS App Intelligence

## Integration test database

Automated tests use the `sis_app_test` database in the same PostgreSQL container as the application, never the development database. The test profile fixes the database name; `POSTGRES_TEST_HOST`, `POSTGRES_TEST_PORT`, `POSTGRES_TEST_USER`, and `POSTGRES_TEST_PASSWORD` can configure the test connection. The test startup initializes the regular schema and `schema-vector.sql`, which enables pgvector in the test database.

Create the test database once if it does not exist. With the repository's Compose service running, connect as the configured PostgreSQL owner (the default user is `sis_app`):

```text
docker compose exec postgres psql -U sis_app -d postgres
```

In `psql`, check whether `sis_app_test` exists; if it does not, create it and enable pgvector:

```sql
SELECT datname FROM pg_database WHERE datname = 'sis_app_test';
CREATE DATABASE sis_app_test OWNER sis_app;
\connect sis_app_test
CREATE EXTENSION IF NOT EXISTS vector;
```

Run `CREATE DATABASE` only when the preceding query returns no rows. If the Compose PostgreSQL user was customized, use that role in the connection and as the database owner. Then run the usual test command, for example `.\mvnw.cmd test`.

## MSSQL metadata source (draft)

The internal metadata adapter performs catalog-only SELECT queries when `POST /api/metadata/sync` is invoked; application startup does not connect to SQL Server. Configure `app.mssql.url`, `app.mssql.username`, and `app.mssql.password` through environment-backed settings in `application-internal.properties`. Never commit live credentials or internal host/database values.

Use a dedicated SQL Server account with only `db_datareader` and `VIEW DEFINITION` grants for the intended database. The adapter does not claim read-only behavior from JDBC connection flags; database permissions enforce the boundary. If procedure definitions are not visible, the sync records `definition_status=NO_ACCESS` and does not fabricate source text.

## AI analysis

`app.ai.provider` defaults to `mock`. `MockAiAnalysisAdapter` is deterministic, performs no network calls, and preserves source citations. The portfolio-only `POST /api/ai/analyze-debug` endpoint accepts a question and up to 10 context chunks for local wiring checks. An optional Gemini analysis adapter is portfolio-only and requires both `APP_AI_PROVIDER=gemini` and `APP_AI_EXTERNAL_ENABLED=true`; only fictional portfolio context may be sent. Internal mode rejects adapters that report external data transfer. No external AI call is required for the portfolio demo.

## Portfolio embeddings

The `portfolio` profile loads `db/schema.sql` and `schema-vector.sql`; the default and `internal` profiles do not load pgvector DDL. `schema-vector.sql` creates the `document_chunk` prerequisite, `chunk_embedding`, and its cosine HNSW index. `POST /api/source-files/scan` collects only the committed fictional portfolio source files, and `POST /api/chunks/rebuild` chunks source, mapper, procedure, and table metadata while preserving citation references.

Mock embeddings are the default and require no external connection. `POST /api/embeddings/rebuild` embeds missing or changed portfolio chunks; append `?force=true` to regenerate all vectors. `GET /api/embeddings/similar?q=repair%20workflow&k=5` returns the nearest chunk metadata and up to 200 content characters. Mock vectors are deterministic test data, not semantic embeddings. Both endpoints are unavailable outside the `portfolio` profile.

Gemini is optional and disabled by default. To opt in, set `APP_AI_EMBEDDING_PROVIDER=gemini`, `APP_AI_EXTERNAL_ENABLED=true`, and `GEMINI_API_KEY` in the environment. It uses `gemini-embedding-001` with 768 dimensions and sends `taskType` and `outputDimensionality` under `embedContentConfig` in each `batchEmbedContents` request. Request fields were checked against the [official Gemini REST schema](https://ai.google.dev/api/embeddings); no live Gemini call has been made.

Manual integration verification (not run by automated tests):

1. Configure the internal settings locally from approved environment variables.
2. Start the application with the `internal` profile and confirm startup succeeds while the SQL Server is unavailable.
3. Call `POST /api/metadata/sync` only after confirming the account grants and network access.
4. Verify successful counts and representative catalog rows in the configured PostgreSQL metadata store; verify missing procedure-definition permission produces `NO_ACCESS`.
5. Remove the local settings after verification. Do not include connection strings, names, credentials, or real source data in logs or screenshots.

## Portfolio MCP tools

The portfolio profile exposes exactly three read-only MCP tools over Spring AI's Streamable HTTP transport at `http://127.0.0.1:8080/mcp`:

- `search_columns`: search fictional portfolio table/column metadata.
- `get_procedure_source`: read a procedure by exact schema and name.
- `get_call_tree`: inspect a procedure call tree with a maximum depth of 10.

The MCP bean and transport are enabled only with the `portfolio` profile. The server binds to loopback because the HTTP MCP endpoint has no built-in authentication. Do not expose or rebind it to a network interface. It accepts only structured search terms, exact procedure identifiers, and a bounded call-tree depth; it does not accept SQL or filesystem paths.

Run against the application's configured portfolio database after starting PostgreSQL and creating its schema:

```powershell
$env:SPRING_PROFILES_ACTIVE = "portfolio"
.\mvnw.cmd spring-boot:run
```

Before invoking tools, populate fictional metadata and dependency edges with `POST /api/metadata/sync`, `POST /api/source-files/scan`, and `POST /api/dependencies/rebuild`. Connect a Streamable HTTP MCP client to `http://127.0.0.1:8080/mcp`, then ask it to find columns, inspect `dbo.USP_COMPLETE_REPAIR_ORDER`, and trace its callees. The MCP server itself makes no model or external API calls. Use a client configured for Mock/local behavior unless an external provider has been separately approved.

## RAG retrieval evaluation

The versioned 20-question portfolio retrieval fixture and repeatable Hit@5/Recall@5 command are documented in [`docs/RAG_EVALUATION.md`](docs/RAG_EVALUATION.md). The integration evaluation requires the test database `sis_app_test`; it must not be run against the development database.
