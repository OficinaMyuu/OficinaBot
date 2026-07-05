# Oficina Registrar

Registrar is the lightweight Go service that owns the legacy `r!` Discord registration flow.

## Runtime
- Entrypoint: `cmd/registrar/main.go`
- Discord library: `github.com/bwmarrin/discordgo`
- Database: MySQL through `database/sql`
- Required DB env: `DATABASE_HOST`, `DATABASE_USER`, and `DATABASE_PASSWORD`
- Optional DB env: `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_COLLATION`, `DATABASE_MAX_POOL_SIZE`, `DATABASE_MIN_IDLE`, `DATABASE_CONNECTION_TIMEOUT_MS`, `DATABASE_VALIDATION_TIMEOUT_MS`, `DATABASE_IDLE_TIMEOUT_MS`, and `DATABASE_MAX_LIFETIME_MS`
- DB-backed config keys: `app.token`, `channels.registry`, and `channels.registry.log`

## Behavior
- `r!revoke <member>` revokes registration for a member.
- Any other `r!` message is treated as a registration command.
- Register patterns keep the old compact syntax: first character is gender (`f`, `m`, `n`), digits are age, and last character is device (`p`, `m`).
- The registry janitor deletes non-staff messages without digits in the registry channel and deletes recent registry messages from users who leave the guild.

## Validation
```sh
go test ./...
go build ./...
```

Set `OFICINA_TEST_MYSQL_DSN` to run the live MySQL repository integration test. The test creates a temporary `registers` table on its own connection and does not mutate product schema.
