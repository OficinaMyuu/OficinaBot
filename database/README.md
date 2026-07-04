# Oficina Database

This module owns the current MySQL migration stream for the main Discord bot.
Backend/dashboard tables are intentionally not part of this stream until the
backend persistence model is redesigned.

Run migrations with a database user that is allowed to execute DDL:

```powershell
$env:DATABASE_HOST="127.0.0.1"
$env:DATABASE_PORT="3306"
$env:DATABASE_NAME="oficina_services"
$env:DATABASE_USER="oficina_migrator"
$env:DATABASE_PASSWORD="..."
go run ./cmd/migrator up
```

Application services must not run schema migrations at startup. The bot connects
to the schema after this migrator has successfully applied the ordered SQL files
in `database/migrations`.

Use separate credentials:

- Migrator user: DDL privileges for schema changes.
- Application users: runtime DML privileges only.

Live integration tests:

- Java bot tests use `OFICINA_TEST_MYSQL_JDBC_URL`, plus optional `OFICINA_TEST_MYSQL_USER` and `OFICINA_TEST_MYSQL_PASSWORD` when credentials are not embedded in the JDBC URL.
