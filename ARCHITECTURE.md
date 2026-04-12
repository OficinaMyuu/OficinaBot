# ARCHITECTURE.md

## Overview
Oficina is a Java 21 Discord bot built on JDA. The application boots from `src/main/java/ofc/bot/Main.java`, initializes SQLite-backed persistence, registers Quartz jobs, then wires Discord listeners and slash commands through centralized registries.

## Main Architectural Pieces
- Entry point:
  `src/main/java/ofc/bot/Main.java`
- Application wiring:
  `src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
- Slash command registration:
  `src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
- Persistence bootstrap:
  `src/main/java/ofc/bot/domain/sqlite/DB.java`
- Repository access:
  `src/main/java/ofc/bot/domain/sqlite/repository/Repositories.java`

## Persistence Shape
- SQLite database file: `database.db`
- Table definitions live under `src/main/java/ofc/bot/domain/tables/`
- Entity models live under `src/main/java/ofc/bot/domain/entity/`
- Repository implementations live under `src/main/java/ofc/bot/domain/sqlite/repository/`
- Runtime configuration is partially stored in the SQLite `config` table and accessed via `BotProperties`

## Interaction Model
- Slash commands live under `src/main/java/ofc/bot/commands/impl/slash/`
- JDA event listeners live under `src/main/java/ofc/bot/listeners/`
- Button/modal/menu/autocomplete routing is centralized under `src/main/java/ofc/bot/handlers/interactions/`
- Scheduled jobs live under `src/main/java/ofc/bot/jobs/`

## Operational Notes
- Build output is a shaded jar at `target/bot.jar`
- CI deploy workflow is defined in `.github/workflows/deploy.yml`
- Local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, and `target/` should not be read for code understanding; they are runtime/generated artifacts, not source of truth
