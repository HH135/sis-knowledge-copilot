# SIS App Intelligence

## MSSQL metadata source (draft)

The internal metadata adapter performs catalog-only SELECT queries when `POST /api/metadata/sync` is invoked; application startup does not connect to SQL Server. Configure `app.mssql.url`, `app.mssql.username`, and `app.mssql.password` through environment-backed settings in `application-internal.properties`. Never commit live credentials or internal host/database values.

Use a dedicated SQL Server account with only `db_datareader` and `VIEW DEFINITION` grants for the intended database. The adapter does not claim read-only behavior from JDBC connection flags; database permissions enforce the boundary. If procedure definitions are not visible, the sync records `definition_status=NO_ACCESS` and does not fabricate source text.

## AI analysis port (mock only)

`app.ai.provider` defaults to `mock`; no other provider is currently accepted. `MockAiAnalysisAdapter` is deterministic, performs no network calls, and preserves source citations. The portfolio-only `POST /api/ai/analyze-debug` endpoint accepts a question and up to 10 context chunks for local wiring checks. External adapters are not included. Any future portfolio adapter must require `app.ai.external-enabled=true`; the internal profile rejects adapters that report external data transfer.

## Portfolio embeddings

The `portfolio` profile loads `db/schema.sql` and `schema-vector.sql`; the default and `internal` profiles do not load pgvector DDL. `schema-vector.sql` creates the `document_chunk` prerequisite, `chunk_embedding`, and its cosine HNSW index. No chunking or chunk-import workflow is included yet, so portfolio chunks must be supplied before rebuilding.

Mock embeddings are the default and require no external connection. `POST /api/embeddings/rebuild` embeds missing or changed portfolio chunks; append `?force=true` to regenerate all vectors. `GET /api/embeddings/similar?q=repair%20workflow&k=5` returns the nearest chunk metadata and up to 200 content characters. Mock vectors are deterministic test data, not semantic embeddings. Both endpoints are unavailable outside the `portfolio` profile.

Gemini is optional and disabled by default. To opt in, set `APP_AI_EMBEDDING_PROVIDER=gemini`, `APP_AI_EXTERNAL_ENABLED=true`, and `GEMINI_API_KEY` in the environment. It uses `gemini-embedding-001` with 768 dimensions and sends `taskType` and `outputDimensionality` under `embedContentConfig` in each `batchEmbedContents` request. Request fields were checked against the [official Gemini REST schema](https://ai.google.dev/api/embeddings); no live Gemini call has been made.

Manual integration verification (not run by automated tests):

1. Configure the internal settings locally from approved environment variables.
2. Start the application with the `internal` profile and confirm startup succeeds while the SQL Server is unavailable.
3. Call `POST /api/metadata/sync` only after confirming the account grants and network access.
4. Verify successful counts and representative catalog rows in the configured PostgreSQL metadata store; verify missing procedure-definition permission produces `NO_ACCESS`.
5. Remove the local settings after verification. Do not include connection strings, names, credentials, or real source data in logs or screenshots.
