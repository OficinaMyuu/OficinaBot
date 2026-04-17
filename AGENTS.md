# AGENTS.md

## Purpose
This file is a low-token index for future agents working in this repository.
Read this first, then open only the files relevant to the task.

## Hard Rules For Exploration
- Never read local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, anything inside `target/`, or other generated binaries. They are local garbage, runtime output, bytecode, or backups.
- Do not inspect `content/*.json` unless the task is explicitly about content payloads. These are environment-specific data files.
- Prefer reading registration/entrypoint files before feature implementations.
- Tests now exist under `src/test/java/ofc/bot/handlers/games/mafia/`; extend them when changing Oficina Dorme behavior.

## Project Snapshot
- Stack: Java 21, Maven, JDA 6, SQLite, jOOQ, HikariCP, Quartz, OkHttp, OpenAI Java SDK.
- App type: Discord bot for one server/community.
- Packaging: shaded jar built as `target/bot.jar`.
- Runtime config is partly database-backed, not `.env`-driven.

## Start Here
- Boot flow: `src/main/java/ofc/bot/Main.java`
- Global registration hub: `src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
- Slash command registration: `src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
- Database bootstrap/schema creation: `src/main/java/ofc/bot/domain/sqlite/DB.java`
- Repository locator: `src/main/java/ofc/bot/domain/sqlite/repository/Repositories.java`
- File/bootstrap paths: `src/main/java/ofc/bot/internal/data/BotFiles.java`
- DB-backed config lookup: `src/main/java/ofc/bot/internal/data/BotProperties.java`
- Logging config: `src/main/resources/logback.xml`

## Boot Flow
1. `Main.main()` loads local files through `BotFiles.loadFiles()`.
2. `DB.init()` creates/connects the SQLite datasource and creates all known tables.
3. JDA is built and awaited.
4. Console handler and Quartz jobs are initialized.
5. Services, listeners, slash commands, and composed interactions are registered.
6. OpenAI client is created from `openai.key`.

## Important Repo Facts
- Secrets/config are fetched through `Bot.getSafe(...)` and `BotProperties`, which query the SQLite `config` table.
- The DB schema is code-first: table definitions live under `src/main/java/ofc/bot/domain/tables/`.
- Persistence follows a simple pattern:
  - entity classes: `src/main/java/ofc/bot/domain/entity/`
  - table definitions: `src/main/java/ofc/bot/domain/tables/`
  - repositories: `src/main/java/ofc/bot/domain/sqlite/repository/`
- Interaction handling is split by transport:
  - slash commands
  - buttons
  - modals
  - autocomplete
  - menus
- Many features are registered centrally, so missing behavior is often a registration problem, not a logic problem.

## Directory Index
- `src/main/java/ofc/bot/commands/`
  Slash command implementations by feature area.
- `src/main/java/ofc/bot/listeners/`
  JDA event listeners, split into guild/log/interaction/moderation/economy areas.
- `src/main/java/ofc/bot/jobs/`
  Quartz scheduled jobs and recurring automation.
- `src/main/java/ofc/bot/handlers/`
  Framework glue, registries, interaction gateways, pagination, moderation, games, groups, economy.
- `src/main/java/ofc/bot/domain/`
  Entities, enums, tables, view models, SQLite bootstrap, repositories.
- `src/main/java/ofc/bot/internal/`
  Internal app data/bootstrap helpers.
- `src/main/java/ofc/bot/util/`
  Shared helpers, content constants, embeds, bot utility accessors.
- `src/main/resources/`
  Logging configuration.
- `.github/workflows/deploy.yml`
  CI build-and-deploy workflow.

## Start Here For Common Tasks
- Add or modify a slash command:
  Open `CommandsInitializer.java`, then the command under `commands/impl/slash/...`.
- Change channel permission optimization:
  Open `ChannelOptimizeCommand.java`, then `handlers/channels/ChannelPermissionOptimizer.java`, and finally `ChannelOptimizeApproveHandler.java`.
- Change Oficina Dorme behavior:
  Open `CreateMafiaGameCommand.java`, then `MafiaInteractionListener.java`, and finally the rule helpers in `handlers/games/mafia/service/`.
- Fix a Discord event reaction:
  Open `EntityInitializerManager.java`, then the relevant listener under `listeners/discord/...`.
- Change button/modal behavior:
  Check `registerComposedInteractions()` in `EntityInitializerManager.java`, then open the matching handler under `listeners/discord/interactions/...`.
- Change scheduled behavior:
  Open `initializeCronJobs()` in `EntityInitializerManager.java`, then the target job in `jobs/`.
- Change persistence or schema:
  Open `DB.java`, then the related table/entity/repository trio under `domain/`.
- Debug config or startup failures:
  Start with `Main.java`, `BotFiles.java`, `BotProperties.java`, and `DB.java`.
- Debug command visibility/registration:
  Start with `CommandsInitializer.java` and `SlashCommandsRegistryManager.java`.

## Feature Map
- Economy:
  `commands/impl/slash/economy/`, `listeners/discord/economy/`, `handlers/economy/`, `UserEconomyRepository`
- Groups:
  `commands/impl/slash/groups/`, `listeners/discord/interactions/buttons/groups/`, `handlers/groups/`, `OficinaGroupRepository`
- Marriage/relationships:
  `commands/impl/slash/relationships/`, `MarriageRepository`, `MarriageRequestRepository`
- Reminders:
  `commands/impl/slash/reminders/`, `jobs/RemindersHandler.java`, `ReminderRepository`
- Moderation:
  `commands/impl/slash/moderation/`, `listeners/discord/moderation/`, `handlers/moderation/`, punishment repositories
- Levels/XP:
  `commands/impl/slash/levels/`, `listeners/discord/guilds/messages/UsersXPHandler.java`, `jobs/income/VoiceXPHandler.java`
- Tickets:
  `commands/impl/slash/tickets/`, modal/button handlers under `listeners/discord/interactions/.../tickets/`
- Mafia/bets/games:
  `commands/impl/slash/mafia/`, `commands/impl/slash/bets/`, `handlers/games/`
- Oficina Dorme internals:
  `handlers/games/mafia/service/` contains orchestration and pure rules,
  `handlers/games/mafia/domain/` contains the in-memory match state,
  `handlers/games/mafia/discord/` contains embeds and components,
  `listeners/discord/interactions/buttons/mafia/MafiaInteractionListener.java` handles both buttons and select menus,
  `listeners/discord/guilds/mafia/MafiaLifecycleListener.java` handles deleted channels and member departures,
  and `game_mafia_logs` stores the persisted audit trail for match events.
- User profile/customization:
  `commands/impl/slash/userinfo/`, `commands/impl/slash/userinfo/custom/`, `CustomUserinfoRepository`
- Channel permission optimization:
  `commands/impl/slash/ChannelOptimizeCommand.java`,
  `handlers/channels/ChannelPermissionOptimizer.java`,
  `listeners/discord/interactions/buttons/channels/ChannelOptimizeApproveHandler.java`,
  `util/embeds/EmbedFactory.java`,
  and `src/test/java/ofc/bot/handlers/channels/ChannelPermissionOptimizerTest.java`

## Central Registration Files Worth Memorizing
- `src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
  Registers jobs, services, listeners, and composed interaction handlers.
- `src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
  Instantiates every slash command and pushes them to Discord.
- `src/main/java/ofc/bot/domain/sqlite/DB.java`
  Creates datasource and all application tables.
- `src/main/java/ofc/bot/domain/sqlite/repository/Repositories.java`
  Shared repository factory/access point.

## Files Usually Safe To Ignore
- `database.db`
- `database-backup.db`
- `bot.jar`
- `target/`
- `.idea/`
- `content/*.json` unless the task explicitly needs them

## Build And Validation
- Build command: `mvn clean package`
- Local test command used for this repo in the sandbox: `mvn "-Dmaven.repo.local=.m2" test`
- CI currently builds with `-DskipTests`, then uploads `target/bot.jar` through SFTP.
- Oficina Dorme now has an automated unit test suite under `src/test/java/ofc/bot/handlers/games/mafia/`.
- For doc-only changes, a file review is enough.

## Known Traps
- Do not assume env files exist; config is often loaded from the DB `config` table.
- Do not assume a missing feature is unimplemented before checking central registration.
- Do not read generated artifacts for context. If it smells like bytecode, backup sludge, or runtime leftovers, leave it alone.
- SQLite is configured with a single pooled connection on purpose; avoid "fixing" that casually.

## Recommended Reading Budget
- First pass:
  `Main.java`, `EntityInitializerManager.java`, `CommandsInitializer.java`, `DB.java`, `Repositories.java`
- Second pass only if needed:
  the specific feature folder plus its repository/entity/table
- Avoid broad repo sweeps unless the task is architectural
