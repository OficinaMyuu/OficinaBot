# Database Migration Audit

Date: 2026-07-01

## Scope

Audit and migration preparation for moving Oficina's main Discord bot from
SQLite-era local persistence to OCI MySQL. The broader repo contains:

- `bot/` general Discord bot
- `registrar/` registration Discord bot
- `backend/` API/dashboard backend

## Decision

Use one centralized, versioned schema migrator for the bot database schema.
Terraform provisions OCI infrastructure only. Application services open pooled
runtime connections only and must not create, alter, or migrate schema at
startup.

This is the industry-standard shape: infrastructure as code owns
infrastructure, a migration runner owns DDL, and application boot owns only
connectivity and health validation. The selected implementation is the root
Go `database/` module using goose with ordered SQL migrations.

Runtime applications should not receive DDL credentials. Use a DDL-capable
migration user only for the migrator, then run the bot with an application user
limited to DML privileges.

## Implemented

- Added `database/` as the centralized migration module.
- Replaced the initial mixed backend/bot migration stream with a bot-only
  migration stream.
- Removed backend startup migration execution.
- Replaced bot SQLite wiring with MySQL Connector/J, jOOQ MySQL dialect, and
  HikariCP.
- Replaced registrar SQLite wiring with MySQL Connector/J, jOOQ MySQL dialect,
  and HikariCP.
- Removed the bot `BotFiles` database file helper.
- Renamed bot persistence packages from `domain.sqlite` to `domain.database`.
- Removed shorthand SQL type aliases from `InitializableTable`; table mappings
  now use explicit `SQLDataType.*` fields.
- Stopped bot and registrar startup from running `CREATE TABLE IF NOT EXISTS`.

## HikariCP Defaults

The bot defaults to:

- `DATABASE_MAX_POOL_SIZE=8`
- `DATABASE_MIN_IDLE=2`
- `DATABASE_CONNECTION_TIMEOUT_MS=10000`
- `DATABASE_VALIDATION_TIMEOUT_MS=5000`
- `DATABASE_IDLE_TIMEOUT_MS=600000`
- `DATABASE_MAX_LIFETIME_MS=1500000`
- `DATABASE_KEEPALIVE_TIME_MS=300000`

The registrar defaults to:

- `DATABASE_MAX_POOL_SIZE=3`
- `DATABASE_MIN_IDLE=1`

Both Java services enable prepared statement caching, server prepared
statements, and batched-statement rewriting.

## Roundtrip Fixes

- Roulette payouts/refunds now use batched user economy updates instead of
  fetch/update loops.
- Mention logging now collects rows and uses one bulk repository call.
- Nickname emoji policy now bulk-loads relevant staff emoji owners and user
  emoji permissions.
- Giveaway voice-condition cleanup now deletes matching entries in one
  repository operation and returns affected giveaway IDs.

## Remaining Smells

- Bot table classes still expose jOOQ `createTableIfNotExists` schema helper
  methods. They are no longer invoked by application startup and are not the
  source of truth. Retire them after Java tests are migrated to apply the
  central SQL migration stream directly.
- `OficinaBankClient` still uses read-modify-write balance updates for several
  flows. Under MySQL concurrency, this should become atomic SQL updates with
  conditional `WHERE` clauses or explicit transactions.
- `ChatMoneyHandler` still performs a per-message fetch and upsert. This is
  acceptable at low volume because it is cooldown-gated, but it should become
  an atomic increment to avoid lost updates.
- `VoiceXPHandler`/`LevelManager` can perform multiple DB reads and writes per
  eligible member. This is less trivial to batch because level-up notifications
  and role grants are side effects, but the XP state update itself should be
  bulk-loaded and batch-upserted.
- Some migration DDL intentionally preserves the old application's weak
  relationship model instead of adding aggressive foreign keys immediately.
  Add stricter constraints only after production data has been profiled and
  cleaned, otherwise the migration can fail on historical rows.

## Validation

Completed:

- `go test ./...` from `database/`
- `mvn -q "-Dmaven.repo.local=../.m2" test` from `bot/`
- `mvn -q "-Dmaven.repo.local=../.m2" test` from `registrar/`

Local live MySQL validation could not be completed on this machine because
Docker Desktop is not running and no local MySQL server/client is installed.
The Java MySQL tests are wired to run when `OFICINA_TEST_MYSQL_JDBC_URL` is
set.

Recommended live checks before deployment:

```powershell
cd database
$env:DATABASE_HOST='host'
$env:DATABASE_PORT='3306'
$env:DATABASE_NAME='oficina_services'
$env:DATABASE_USER='migration_user'
$env:DATABASE_PASSWORD='password'
go run ./cmd/migrator status
go run ./cmd/migrator up
```

```powershell
cd bot
$env:OFICINA_TEST_MYSQL_JDBC_URL='jdbc:mysql://host:3306/oficina_test?serverTimezone=UTC'
$env:OFICINA_TEST_MYSQL_USER='test_user'
$env:OFICINA_TEST_MYSQL_PASSWORD='password'
mvn "-Dmaven.repo.local=../.m2" test
```

Backend/database integration tests are intentionally excluded until the backend
schema is redesigned against the shared MySQL model.
