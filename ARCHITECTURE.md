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

## Oficina Dorme
- Slash entrypoint:
  `src/main/java/ofc/bot/commands/impl/slash/mafia/CreateMafiaGameCommand.java`
- Runtime orchestration:
  `src/main/java/ofc/bot/handlers/games/mafia/service/MafiaGameManager.java`
- Audit logging:
  `src/main/java/ofc/bot/handlers/games/mafia/service/MafiaGameLogger.java`
- Pure game rules:
  `src/main/java/ofc/bot/handlers/games/mafia/service/MafiaMatchEngine.java`
- Discord message/component builders:
  `src/main/java/ofc/bot/handlers/games/mafia/discord/`
- Live interaction listener:
  `src/main/java/ofc/bot/listeners/discord/interactions/buttons/mafia/MafiaInteractionListener.java`
- Passive lifecycle listener:
  `src/main/java/ofc/bot/listeners/discord/guilds/mafia/MafiaLifecycleListener.java`
- Unit tests:
  `src/test/java/ofc/bot/handlers/games/mafia/`

The Oficina Dorme mini-game is in-memory and channel-scoped. Each match is keyed by the main event text channel, while private role coordination happens in private threads created under that same channel. The rules layer is intentionally separated from the Discord layer so round resolution, role balancing, vote ties, and victory conditions can be verified with unit tests.

Operationally, the feature has a hard cap of 25 players because Discord string select menus support at most 25 options and the match uses select menus for all automated voting. If any required action channel is deleted, the match is terminated immediately. If a participant leaves the guild, is kicked, or is banned, they are removed from the in-memory roster, outstanding votes targeting them are invalidated, and the match is re-evaluated for immediate victory or phase progression.

Every relevant match action is also persisted to `game_mafia_logs` through `GameMafiaLogRepository`, using an English `action` string plus structured fields such as event type, actor, target, channel, phase, and timestamp so moderation can inspect the full match timeline later.

## Operational Notes
- Build output is a shaded jar at `target/bot.jar`
- CI deploy workflow is defined in `.github/workflows/deploy.yml`
- Local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, and `target/` should not be read for code understanding; they are runtime/generated artifacts, not source of truth
