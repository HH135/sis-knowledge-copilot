# Portfolio RAG retrieval evaluation

Run the versioned, fictional portfolio retrieval set against the isolated `sis_app_test` database:

```powershell
.\mvnw.cmd '-Dtest=RetrievalEvaluationTest' '-Dsurefire.useFile=false' test
```

The integration test activates only the `test` and `portfolio` profiles, requires PostgreSQL's current database to be exactly `sis_app_test`, and forces deterministic Mock embeddings with external AI disabled. It rebuilds portfolio metadata, source chunks, and embeddings within a transaction that is rolled back after the test. It does not use the internal profile, real SIS data, a development database, or an external AI API.

The test evaluates Hybrid retrieval at **k = 5** using `src/test/resources/rag/retrieval-evaluation-v1.json` (20 questions). For each question, **Hit@5** is 1 when at least one expected `sourceRef` occurs in the first five retrieved chunks, otherwise 0. **Recall@5** is the fraction of distinct expected `sourceRef` values present in those results. The command keeps Surefire output on the console: every question's scores and the final macro-averaged Hit@5 and Recall@5, question count, and fixture version are printed.
