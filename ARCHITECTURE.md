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
- Shared utility helpers:
  `src/main/java/ofc/bot/util/`

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

## Channel Permission Optimization
- Slash entrypoint:
  `src/main/java/ofc/bot/commands/impl/slash/ChannelOptimizeCommand.java`
- Optimization engine:
  `src/main/java/ofc/bot/handlers/channels/ChannelPermissionOptimizer.java`
- Approval button handler:
  `src/main/java/ofc/bot/listeners/discord/interactions/buttons/channels/ChannelOptimizeApproveHandler.java`
- Shared embeds/buttons:
  `src/main/java/ofc/bot/util/embeds/EmbedFactory.java`
  and `src/main/java/ofc/bot/handlers/interactions/EntityContextFactory.java`
- Unit tests:
  `src/test/java/ofc/bot/handlers/channels/ChannelPermissionOptimizerTest.java`

`/chanoptz` is a review-first flow. It requires a target channel parameter, loads every guild member, snapshots the channel overrides, validates a local permission simulation against JDA's explicit channel permissions/access for the current state, and only proposes removals that keep every member's access and explicit channel permission set unchanged. The heavy analysis runs on virtual threads, and the review summary reports both the total number of redundant permission entries found and the optimization percentage. The approval step is guarded by an in-memory review plan plus an override signature check so stale reviews are rejected instead of applying against a changed channel.

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

## Shared Utilities
- `src/main/java/ofc/bot/util/UrlBuilder.java`

`UrlBuilder` is a small query-string utility for features that need to safely inspect or mutate URLs without hand-splicing strings. It preserves the original URI structure, stores decoded query parameters in insertion order, supports fluent updates through `set`, `add`, `remove`, and `clear`, and can build either a `URI` via `toUri()` or a string via `build()` and `toString()`. The utility is intentionally single-value per key; if a feature needs repeated query keys, extend it deliberately instead of quietly changing its semantics.
