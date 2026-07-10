# Oficina Database

This module owns the current MySQL migration stream for the main Discord bot.
Dashboard features reuse bot-owned product tables, including `birthdays`,
`support_tickets`, `messages_versions`, `users`, and `store_item_settings`; the
backend still must not run migrations or create tables at startup. The
`store_item_settings` rows are migration-seeded code-owned action prices, not a
user-facing store catalog.

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

For `store_item_settings`, the bot needs `SELECT` and the dashboard backend needs
`SELECT` plus `UPDATE`; neither runtime user should receive `INSERT` or `DELETE`
for this code-owned configuration table. A missing row is a deployment/configuration
fault to repair through a migration or controlled administrative operation, never by
silently restoring a hard-coded price in the bot.

Live integration tests:

- Java bot tests use `OFICINA_TEST_MYSQL_JDBC_URL`, plus optional `OFICINA_TEST_MYSQL_USER` and `OFICINA_TEST_MYSQL_PASSWORD` when credentials are not embedded in the JDBC URL.
