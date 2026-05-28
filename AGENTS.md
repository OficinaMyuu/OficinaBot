# AGENTS.md

## Purpose
This is the root index for agents working in the OficinaServices mono-repo. Keep this file as the single agent guide for the whole repository; do not add child `AGENTS.md` files.

## Layout
- `bot/` contains the Oficina Discord bot, formerly `OficinaMyuu/OficinaBot`.
- `backend/` contains the Go backend, formerly `OficinaMyuu/OficinaImagery`.
- `registrar/` contains the registration Discord service, formerly `OficinaMyuu/RegistroOficina`.
- `.github/workflows/` contains repo-level CI/deploy workflows. Keep workflows at the repository root so GitHub Actions can discover them.

## Hard Rules For Exploration
- Never read local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, anything inside `target/`, generated binaries, or local backups.
- Do not inspect `bot/content/*.json` unless the task is explicitly about content payloads. These are environment-specific data files.
- Prefer service entrypoints and registration files before feature implementations.
- Keep changes scoped to the relevant service unless the task is explicitly cross-service.

## Service Entry Points
- Bot boot flow: `bot/src/main/java/ofc/bot/Main.java`
- Bot registration hub: `bot/src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
- Bot slash command registration: `bot/src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
- Bot DB bootstrap/schema creation: `bot/src/main/java/ofc/bot/domain/sqlite/DB.java`
- Bot repository locator: `bot/src/main/java/ofc/bot/domain/sqlite/repository/Repositories.java`
- Bot file/bootstrap paths: `bot/src/main/java/ofc/bot/internal/data/BotFiles.java`
- Bot DB-backed config lookup: `bot/src/main/java/ofc/bot/internal/data/BotProperties.java`
- Backend entrypoint: `backend/cmd/api/main.go`; application setup lives under `backend/cmd/internal/app/`; admin and service auth live under `backend/cmd/internal/auth/`; persistence lives under `backend/cmd/internal/database/` and `backend/cmd/internal/repository/`; routes live under `backend/cmd/internal/routes/`.
- Registrar entrypoint: `registrar/src/main/java/ofc/bot/RegisterMaster.java`.

## Bot Project Snapshot
- Stack: Java 21, Maven, JDA 6, SQLite, jOOQ, HikariCP, Quartz, OkHttp, OpenAI Java SDK.
- App type: Discord bot for one server/community.
- Packaging: shaded jar built as `bot/target/bot.jar`.
- Runtime config is partly database-backed, not `.env`-driven.
- Secrets/config are fetched through `Bot.getSafe(...)` and `BotProperties`, which query the SQLite `config` table.
- The DB schema is code-first: table definitions live under `bot/src/main/java/ofc/bot/domain/tables/`.
- Schema migrations are manual for this project. Do not add automatic migration logic to `DB.java`.
- Many features are registered centrally, so missing behavior is often a registration problem, not a logic problem.

## Bot Directory Index
- `bot/src/main/java/ofc/bot/commands/`: slash command implementations by feature area.
- `bot/src/main/java/ofc/bot/listeners/`: JDA event listeners, split into guild/log/interaction/moderation/economy areas.
- `bot/src/main/java/ofc/bot/jobs/`: Quartz scheduled jobs and recurring automation.
- `bot/src/main/java/ofc/bot/handlers/`: framework glue, registries, interaction gateways, pagination, moderation, games, groups, economy.
- `bot/src/main/java/ofc/bot/domain/`: entities, enums, tables, view models, SQLite bootstrap, repositories.
- `bot/src/main/java/ofc/bot/internal/`: internal app data/bootstrap helpers.
- `bot/src/main/java/ofc/bot/util/`: shared helpers, content constants, embeds, bot utility accessors.
- `bot/src/main/resources/`: logging configuration.

## Start Here For Common Bot Tasks
- Add or modify a slash command: open `CommandsInitializer.java`, then the command under `bot/src/main/java/ofc/bot/commands/impl/slash/...`.
- Change color role shop behavior: open `ColorsCommand.java`, `ColorRoleStoreMessageFactory.java`, `ColorRoleRefundPolicy.java`, and the shop button handlers under `bot/src/main/java/ofc/bot/listeners/discord/interactions/buttons/shop/`.
- Change channel permission optimization: open `ChannelOptimizeCommand.java`, `ChannelPermissionOptimizer.java`, and `ChannelOptimizeApproveHandler.java`.
- Change Oficina Dorme behavior: open `CreateMafiaGameCommand.java`, `MafiaInteractionListener.java`, and the rule helpers in `bot/src/main/java/ofc/bot/handlers/games/mafia/service/`.
- Change giveaway behavior: open `bot/src/main/java/ofc/bot/commands/impl/slash/giveaway/`, then `bot/src/main/java/ofc/bot/handlers/giveaway/`, `GiveawayInteractionListener.java`, `GiveawayVoiceConditionListener.java`, and `GiveawayEndHandler.java`.
- Change persistence or schema: open `DB.java`, then the related table/entity/repository trio under `bot/src/main/java/ofc/bot/domain/`.
- Debug config or startup failures: start with `Main.java`, `BotFiles.java`, `BotProperties.java`, and `DB.java`.
- Debug command visibility/registration: start with `CommandsInitializer.java` and `SlashCommandsRegistryManager.java`.

## Bot Feature Map
- Economy: `commands/impl/slash/economy/`, `listeners/discord/economy/`, `handlers/economy/`, `UserEconomyRepository`. `/rob` steals wallet only and fines bank on failure. `PolicyType.BLOCK_MONEY_GAINS` blocks automated money earnings only.
- Color roles: `/colors` Components V2 store lives in `commands/impl/slash/colors/`; rendering lives in `handlers/shop/ColorRoleStoreMessageFactory.java`; final buy/remove actions stay in the shop button handlers.
- Groups: `commands/impl/slash/groups/`, `listeners/discord/interactions/buttons/groups/`, `handlers/groups/`, `OficinaGroupRepository`.
- Marriage/relationships: `commands/impl/slash/relationships/`, `MarriageRepository`, `MarriageRequestRepository`.
- Reminders: `commands/impl/slash/reminders/`, `jobs/RemindersHandler.java`, `ReminderRepository`.
- Moderation: `commands/impl/slash/moderation/`, `listeners/discord/moderation/`, `handlers/moderation/`, punishment repositories.
- Events: `ToggleEventsCommand.java`, configured by `channels.events.text.id` and `channels.events.voice.id`.
- Levels/XP: `commands/impl/slash/levels/`, `UsersXPHandler.java`, `VoiceXPHandler.java`, `LevelManager.java`.
- Automated money income: `ChatMoneyHandler.java`, `VoiceChatMoneyHandler.java`, and `AutomatedMoneyGainPolicy.java`.
- Tickets: `commands/impl/slash/tickets/`, modal/button handlers under `listeners/discord/interactions/.../tickets/`.
- Mafia/bets/games: `commands/impl/slash/mafia/`, `commands/impl/slash/bets/`, `handlers/games/`.
- Giveaways: `/giveaway create/end/reroll`, `handlers/giveaway/`, `GiveawayInteractionListener.java`, `GiveawayVoiceConditionListener.java`, `GiveawayEndHandler.java`, and the `giveaways`, `giveaway_entries`, and `giveaway_winners` tables.
- Oficina Dorme internals: `handlers/games/mafia/service/`, `handlers/games/mafia/domain/`, `handlers/games/mafia/discord/`, `MafiaInteractionListener.java`, `MafiaLifecycleListener.java`, and `game_mafia_logs`.
- Nickname changes: `NickCommand.java`, `handlers/nick/`, `NicknameUpdateRequestGuard.java`, `listeners/discord/interactions/buttons/nick/`, and `nickname_update_requests`.
- Generic throttled updates: `handlers/ThrottledAction.java`.

## Builds And Tests
- Bot tests: run `mvn "-Dmaven.repo.local=../.m2" test` from `bot/`.
- Bot package: run `mvn clean package` from `bot/`.
- Registrar package: run `mvn clean package` from `registrar/`.
- Backend tests: run `go test ./...` from `backend/cmd/`.
- Backend DB tests use real temporary SQLite files and apply embedded goose migrations.
- For doc-only changes, a file review is enough.

## Deployments
- Bot deploy workflow: `.github/workflows/deploy.yml`.
- Registrar deploy workflow: `.github/workflows/deploy-registrar.yml`.
- CodeQL workflow: `.github/workflows/codeql.yml`; scans Java/Kotlin and Go with explicit monorepo build steps.
- Backend deployment is intentionally not wired at the mono-repo root yet.
- Backend persistence defaults to `backend/cmd/data/oficina-services.db` when run from `backend/cmd/`; override with `DATABASE_PATH`.
- Backend admin auth requires `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_REDIRECT_URL`, `OFICINA_OWNER_DISCORD_ID`, and `SESSION_SECRET`.
- Backend Discord REST metadata requires `DISCORD_BOT_TOKEN`; the backend must not call `discordgo.Session.Open()` or otherwise connect to the gateway.
- Use `SESSION_COOKIE_SECURE=false` only for local HTTP development; production cookies should remain secure.
- Backend service APIs live under `/api/service/*` and require `Authorization: Bearer <token>`; only token hashes are stored in `bot_clients`.
- Backend dashboard APIs live under `/api/dashboard/*` and use the admin session cookie.
- Service batch ingestion endpoints require caller-provided `batch_id` values and treat duplicate batches as successful no-ops.
- Backend CORS defaults to `FRONTEND_ORIGIN=http://localhost:5173`, body limit defaults to `BODY_LIMIT=8M`, and cookie-backed mutating admin routes require CSRF headers.
- Bot deployment secrets are service-scoped with the `OFICINA` segment, such as `SFTP_OFICINA_HOST` and `PTERO_OFICINA_SERVER_ID`.
- Registrar deployment secrets are service-scoped with the `REGISTRY` segment, such as `SFTP_REGISTRY_HOST` and `PTERO_REGISTRY_SERVER_ID`.
- `PTERO_API_KEY` remains shared unless a future deployment split requires service-specific API keys.

## Known Traps
- Do not assume env files exist; bot config is often loaded from the DB `config` table.
- Do not assume a missing bot feature is unimplemented before checking central registration.
- SQLite is configured with a single pooled connection on purpose; avoid "fixing" that casually.
- Giveaway buttons are durable component ids prefixed with `giveaway:` and must not use `InteractionMemoryManager`.
- Color role ownership uses `color_roles_state.expires_at`; do not reintroduce fixed `updated_at + 60 days` expiration logic.
- `ColorRoleRemotionHandler` deletes stale `color_roles_state` rows when an expired row points at a Discord role that no longer exists.

## Documentation
- Keep only this root `AGENTS.md`; do not add service-level copies.
- Update this file when changing mono-repo structure, shared workflows, service entrypoints, or feature ownership.
