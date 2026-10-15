# SIS App Intelligence

## MSSQL metadata source (draft)

The internal metadata adapter performs catalog-only SELECT queries when `POST /api/metadata/sync` is invoked; application startup does not connect to SQL Server. Configure `app.mssql.url`, `app.mssql.username`, and `app.mssql.password` through environment-backed settings in `application-internal.properties`. Never commit live credentials or internal host/database values.

Use a dedicated SQL Server account with only `db_datareader` and `VIEW DEFINITION` grants for the intended database. The adapter does not claim read-only behavior from JDBC connection flags; database permissions enforce the boundary. If procedure definitions are not visible, the sync records `definition_status=NO_ACCESS` and does not fabricate source text.

Manual integration verification (not run by automated tests):

1. Configure the internal settings locally from approved environment variables.
2. Start the application with the `internal` profile and confirm startup succeeds while the SQL Server is unavailable.
3. Call `POST /api/metadata/sync` only after confirming the account grants and network access.
4. Verify successful counts and representative catalog rows in the configured PostgreSQL metadata store; verify missing procedure-definition permission produces `NO_ACCESS`.
5. Remove the local settings after verification. Do not include connection strings, names, credentials, or real source data in logs or screenshots.
