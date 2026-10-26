# Portfolio RAG retrieval evaluation

Run the versioned, fictional portfolio retrieval set against the isolated `sis_app_test` database:

```powershell
.\mvnw.cmd '-Dtest=RetrievalEvaluationTest' '-Dsurefire.useFile=false' test
```

The integration test activates only the `test` and `portfolio` profiles, requires PostgreSQL's current database to be exactly `sis_app_test`, and forces deterministic Mock embeddings with external AI disabled. It rebuilds portfolio metadata, source chunks, and embeddings within a transaction that is rolled back after the test. It does not use the internal profile, real SIS data, a development database, or an external AI API.

The test evaluates Hybrid retrieval at **k = 5** using `src/test/resources/rag/retrieval-evaluation-v1.json` (20 questions). For each question, **Hit@5** is 1 when at least one expected `sourceRef` occurs in the first five retrieved chunks, otherwise 0. **Recall@5** is the fraction of distinct expected `sourceRef` values present in those results. The command keeps Surefire output on the console: every question's scores and the final macro-averaged Hit@5 and Recall@5, question count, and fixture version are printed.

## Recorded baseline

With fixture `1.0.0` and the deterministic `mock-token-hash-768` embedding, the recorded macro baseline is **Hit@5 0.950 / Recall@5 0.950** (19 of 20 questions hit). The remaining miss is `repair-order-service`, whose expected source is `portfolio-data/app/java/com/acme/svc/repairorder/RepairOrderService.java`; it ranked 12th in FTS and 11th in vector retrieval, outside the top five.

This records the current portfolio-demo baseline, not a quality acceptance threshold. Mock token-hash vectors provide lexical similarity only and are not semantic embeddings, so these scores do not establish production retrieval quality. Re-run the command after changing retrieval, embeddings, or the fixture, and compare against this baseline.
