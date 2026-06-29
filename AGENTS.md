# AGENTS.md

## Purpose
This is the root index for agents working in the OficinaServices mono-repo. Keep this file as the single agent guide for the whole repository; do not add child `AGENTS.md` files.

## Layout
- `bot/` contains the Oficina Discord bot, formerly `OficinaMyuu/OficinaBot`.
- `backend/` contains the Go backend, formerly `OficinaMyuu/OficinaImagery`.
- `backend/terraform/` contains the OCI Terraform source for backend infrastructure.
- `registrar/` contains the registration Discord service, formerly `OficinaMyuu/RegistroOficina`.
- `.github/workflows/` contains repo-level CI/deploy workflows. Keep workflows at the repository root so GitHub Actions can discover them.

## Hard Rules For Exploration
- Never read local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, anything inside `target/`, generated binaries, or local backups.
- Prefer service entrypoints and registration files before feature implementations.
- Keep changes scoped to the relevant service unless the task is explicitly cross-service.

## Service Entry Points
- Bot boot flow: `bot/src/main/java/ofc/bot/Main.java`
- Bot registration hub: `bot/src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
- Bot slash command registration: `bot/src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
- Bot DB bootstrap/schema creation: `bot/src/main/java/ofc/bot/domain/sqlite/DB.java`
- Bot repository locator: `bot/src/main/java/ofc/bot/domain/sqlite/repository/Repositories.java`
- Bot DB file path: `bot/src/main/java/ofc/bot/internal/data/BotFiles.java`
- Bot DB-backed config lookup: `bot/src/main/java/ofc/bot/internal/data/BotProperties.java`
- Backend entrypoint: `backend/cmd/api/main.go`; application setup lives under `backend/cmd/internal/app/`; admin and service auth live under `backend/cmd/internal/auth/`; persistence lives under `backend/cmd/internal/database/` and `backend/cmd/internal/repository/`; routes live under `backend/cmd/internal/routes/`.
- Backend Terraform entrypoint: `backend/terraform/`; the root module wires shared provider/backend/data concerns, while resources are split under `backend/terraform/modules/`.
- Registrar entrypoint: `registrar/src/main/java/ofc/bot/RegisterMaster.java`.

## Bot Project Snapshot
- Stack: Java 21, Maven, JDA 6, SQLite, jOOQ, HikariCP, Quartz, OkHttp, OpenAI Java SDK.
- App type: Discord bot for one server/community.
- Packaging: shaded jar built as `bot/target/bot.jar`.
- Runtime config is partly database-backed, not `.env`-driven.
- Secrets/config are fetched through `Bot.getSafe(...)` and `BotProperties`, which query the SQLite `config` table.
- Sad Monday/Sunday image posts are configured with `SAD_MONDAY_URL` and `SAD_SUNDAY_URL` environment variables.
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
- Change roulette betting behavior: open `BetRouletteCommand.java` and the roulette helpers under `bot/src/main/java/ofc/bot/handlers/games/betting/roulette/`.
- Change blackjack betting behavior: open `BetBlackjackCommand.java`, `BlackjackActionHandler.java`, and the blackjack helpers under `bot/src/main/java/ofc/bot/handlers/games/betting/blackjack/`.
- Change giveaway behavior: open `bot/src/main/java/ofc/bot/commands/impl/slash/giveaway/`, then `bot/src/main/java/ofc/bot/handlers/giveaway/`, `GiveawayInteractionListener.java`, `GiveawayVoiceConditionListener.java`, and `GiveawayEndHandler.java`.
- Change accumulator prize behavior: open `bot/src/main/java/ofc/bot/commands/impl/slash/accumulator/`, `bot/src/main/java/ofc/bot/handlers/accumulator/`, `AccumulatorInteractionListener.java`, and the `accumulator_prizes` table/repository.
- Change persistence or schema: open `DB.java`, then the related table/entity/repository trio under `bot/src/main/java/ofc/bot/domain/`.
- Debug config or startup failures: start with `Main.java`, `BotFiles.java`, `BotProperties.java`, and `DB.java`.
- Debug command visibility/registration: start with `CommandsInitializer.java` and `SlashCommandsRegistryManager.java`.

## Bot Feature Map
- Economy: `commands/impl/slash/economy/`, `listeners/discord/economy/`, `handlers/economy/`, `UserEconomyRepository`. `/rob` steals wallet only and fines bank on failure. `PolicyType.BLOCK_MONEY_GAINS` blocks automated money earnings only.
- Color roles: `/colors` Components V2 store lives in `commands/impl/slash/colors/`; rendering lives in `handlers/shop/ColorRoleStoreMessageFactory.java`; final buy/remove actions stay in the shop button handlers.
- Accumulator prizes: `/accumulator add/import/list` live in `commands/impl/slash/accumulator/`; money prizes default to the UnbelievaBoat economy; import reads newline-separated user IDs from a same-channel message and can forbid duplicates against both the import payload and pending prizes. Durable list controls live in `listeners/discord/interactions/buttons/accumulator/`; rendering, import planning, and payout orchestration live in `handlers/accumulator/`; rows live in `accumulator_prizes`.
- Groups: `commands/impl/slash/groups/`, `listeners/discord/interactions/buttons/groups/`, `handlers/groups/`, `OficinaGroupRepository`. Group emojis are unique across groups. Role emoji display is stored on `groups.has_role_emoji`; when enabled the role name is `{emoji}⠀⠀⠀⠀{name}⠀⠀⠀⠀{emoji}`, otherwise it is `⠀⠀⠀⠀⠀⠀{name}⠀⠀⠀⠀⠀⠀`.
- Marriage/relationships: `commands/impl/slash/relationships/`, `MarriageRepository`, `MarriageRequestRepository`.
- Userinfo: `commands/impl/slash/userinfo/`, with the counting punishment release buttons under `listeners/discord/interactions/buttons/userinfo/`; the release role id is read from `fun.counting.punishments.role.id` and purchases charge bank through `PaymentManagerProvider`. The release button is shown only when a member views their own `/userinfo` and currently has the configured role.
  Member join history is stored in `member_join_events`; `/userinfo` shows the earliest known join event and falls back to JDA's current member join timestamp when no row exists yet. Live joins are recorded by `MemberJoinUpsert`.
- Reminders: `commands/impl/slash/reminders/`, `jobs/RemindersHandler.java`, `ReminderRepository`.
- Moderation: `commands/impl/slash/moderation/`, `listeners/discord/moderation/`, `handlers/moderation/`, punishment repositories.
- Events: `ToggleEventsCommand.java`, configured by `channels.events.text.id` and `channels.events.voice.id`.
- Message transcriptions: `listeners/discord/guilds/messages/MessageTranscriptionsHandler.java`; users listed in `messages.transcriptions.banned-user-ids` do not receive automatic microphone reactions on voice messages and cannot have those voice messages transcribed through manual microphone reactions.
- Role member lookup: `commands/impl/slash/RoleMembersCommand.java`; `/rolemembers` keeps the response syntax as aligned `id -> username` rows and sorts rows alphabetically by Discord username before sending inline text or the large-result file fallback.
- Attachment forwarding log: `listeners/discord/logs/messages/AttachmentForwardingLogger.java`; user-sent guild messages with attachments are forwarded to the text channel configured by `channels.attachments-log.id` through Discord's native message forward action.
- World Cup 2026 reaction role: `listeners/discord/guilds/reactionroles/WorldCup2026ReactionRoleHandler.java`; messages in `worldcup2026.channel_id` receive a soccer ball reaction, and users who add/remove that reaction are idempotently granted/removed from `worldcup2026.role_id`.
- Levels/XP: `commands/impl/slash/levels/`, `UsersXPHandler.java`, `VoiceXPHandler.java`, `LevelManager.java`. Voice XP channel payout overrides live in `voice_channel_income_rules` with `payout_type = LEVEL_EXPERIENCE`.
- Automated money income: `ChatMoneyHandler.java`, `VoiceChatMoneyHandler.java`, and `AutomatedMoneyGainPolicy.java`. Voice channels listed in `income.voice.bank-channel-ids` pay UnbelievaBoat income to bank with the voice multiplier instead of cash. Voice money channel payout overrides live in `voice_channel_income_rules` with `payout_type = MONEY`.
- Tickets: `commands/impl/slash/tickets/`, modal/button handlers under `listeners/discord/interactions/.../tickets/`.
  Initial ticket messages expose durable add/remove member buttons plus close; member add/remove handling lives in `listeners/discord/interactions/buttons/tickets/TicketMemberManagementHandler.java`. Managing ticket members is restricted to users with `Manage Server` or Support Superior-or-higher staff roles.
- Mafia/bets/games: `commands/impl/slash/mafia/`, `commands/impl/slash/bets/`, `handlers/games/`. `/bets roulette` uses a timed channel lobby, bank-only stakes, and UnbelievaBoat-style roulette spaces under `handlers/games/betting/roulette/`. A user may have only one active roulette entry per lobby; repeating the command replaces that user's previous entry. `/bets blackjack` is single-player against the bot/dealer under `handlers/games/betting/blackjack/`; it is not player-vs-player.
- Giveaways: `/giveaway create/end/reroll`, `handlers/giveaway/`, `GiveawayInteractionListener.java`, `GiveawayVoiceConditionListener.java`, `GiveawayEndHandler.java`, and the `giveaways`, `giveaway_entries`, and `giveaway_winners` tables.
- Oficina Dorme internals: `handlers/games/mafia/service/`, `handlers/games/mafia/domain/`, `handlers/games/mafia/discord/`, `MafiaInteractionListener.java`, `MafiaLifecycleListener.java`, and `game_mafia_logs`.
- Nickname changes: `NickCommand.java`, `handlers/nick/`, `NicknameUpdateRequestGuard.java`, `listeners/discord/guilds/members/*NicknameEmojiEnforcementListener.java`, `listeners/discord/interactions/buttons/nick/`, and `nickname_update_requests`.
- Generic throttled updates: `handlers/ThrottledAction.java`.

## Builds And Tests
- Bot tests: run `mvn "-Dmaven.repo.local=../.m2" test` from `bot/`.
- Bot package: run `mvn clean package` from `bot/`.
- Registrar package: run `mvn clean package` from `registrar/`.
- Backend tests: run `go test ./...` from `backend/cmd/`.
- Backend Terraform validation: run `terraform fmt -check -recursive`, `terraform init -backend=false`, and `terraform validate` from `backend/terraform/`.
- Backend DB tests use real temporary SQLite files and apply embedded goose migrations.
- For doc-only changes, a file review is enough.

## Deployments
- Bot deploy workflow: `.github/workflows/deploy.yml`.
- Registrar deploy workflow: `.github/workflows/deploy-registrar.yml`.
- CodeQL workflow: `.github/workflows/codeql.yml`; scans Java/Kotlin and Go with explicit monorepo build steps.
- Backend deployment is intentionally not wired at the mono-repo root yet.
- Backend Terraform workflow: `.github/workflows/backend-terraform.yml`; pull requests validate without secrets, pushes to `main` plan against the OCI backend, and applies require manual dispatch with the `backend-infra` environment.
- Backend Terraform state backend values are not committed. Use an ignored `backend.oci.tfbackend` locally, and GitHub secrets `OCI_OBJECT_STORAGE_NAMESPACE` and `OCI_TF_STATE_BUCKET` in CI.
- Backend Terraform is constrained for OCI Always Free: `VM.Standard.E2.1.Micro`, 10 Mbps flexible load balancer, `MySQL.Free`, 50 GB MySQL storage, and default 50 GB compute boot volumes.
- Backend Terraform currently exposes the OCI load balancer as public IPv4 HTTP-only. Add HTTPS later through Cloudflare DNS/proxying, Cloudflare Origin CA material on the OCI load balancer, and a 443 listener.
- Backend persistence defaults to `backend/cmd/data/oficina-services.db` when run from `backend/cmd/`; override with `DATABASE_PATH`.
- Backend admin auth requires `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_REDIRECT_URL`, `OFICINA_OWNER_DISCORD_ID`, and `SESSION_SECRET`.
- Backend Discord REST metadata requires `DISCORD_BOT_TOKEN`; the backend must not call `discordgo.Session.Open()` or otherwise connect to the gateway.
- Use `SESSION_COOKIE_SECURE=false` only for local HTTP development; production cookies should remain secure.
- Backend service APIs live under `/api/service/*` and require `Authorization: Bearer <token>`; only token hashes are stored in `bot_clients`.
- Backend dashboard APIs live under `/api/dashboard/*` and use the admin session cookie.
- Backend liveness is exposed by unauthenticated `GET /health`; the Docker image and compose file use this endpoint for container health checks.
- Service batch ingestion endpoints require caller-provided `batch_id` values and treat duplicate batches as successful no-ops.
- Backend CORS defaults to `FRONTEND_ORIGIN=http://localhost:5173`, body limit defaults to `BODY_LIMIT=8M`, and cookie-backed mutating admin routes require CSRF headers.
- Bot deployment secrets are service-scoped with the `OFICINA` segment, such as `SFTP_OFICINA_HOST` and `PTERO_OFICINA_SERVER_ID`.
- Registrar deployment secrets are service-scoped with the `REGISTRY` segment, such as `SFTP_REGISTRY_HOST` and `PTERO_REGISTRY_SERVER_ID`.
- `PTERO_API_KEY` remains shared unless a future deployment split requires service-specific API keys.

## Known Traps
- Do not assume env files exist; bot config is often loaded from the DB `config` table.
- Voice income bank-channel overrides are configured through `income.voice.bank-channel-ids` as semicolon-separated Discord channel IDs; do not hard-code channel snowflakes in `VoiceChatMoneyHandler`.
- Voice channel income customizations are database-backed in `voice_channel_income_rules`, keyed by `(channel_id, payout_type)`. Use `multiplier = 1.25`, `allow_muted = true`, and `allow_solo = true` for event channels that should pay 125% to muted or solo undeafened humans.
- Message transcription author bans are configured through `messages.transcriptions.banned-user-ids` as Discord user IDs returned by `Bot.getArray(...)`; this is separate from `AppUserBanRepository`, which blocks requesters from using bot actions.
- Coinflip inference channel bans are configured through `messages.coinflip.banned-channel-ids` as Discord channel IDs returned by `Bot.getArray(...)`; banned channels are ignored before pending flips or cooldowns are updated.
- Attachment forwarding uses `channels.attachments-log.id` as a Discord text channel ID. It is stateless: do not add a table, scan archive history, or re-upload attachment bytes unless the preservation policy is deliberately changed.
- Do not assume a missing bot feature is unimplemented before checking central registration.
- SQLite is configured with a single pooled connection on purpose; avoid "fixing" that casually.
- Giveaway buttons are durable component ids prefixed with `giveaway:` and must not use `InteractionMemoryManager`.
- Accumulator controls are durable component ids prefixed with `acc:v1:` and must not use `InteractionMemoryManager`; accumulator rows are never deleted, only moved from `PENDING` to `PAID` or `REJECTED`.
- Color role ownership uses `color_roles_state.expires_at`; do not reintroduce fixed `updated_at + 60 days` expiration logic.
- `ColorRoleRemotionHandler` deletes stale `color_roles_state` rows when an expired row points at a Discord role that no longer exists.
- Roulette stakes are deducted from bank when accepted, and replacing an active entry releases the previous reserved stake before reserving the new one. Winners receive `stake * multiplier` back to bank when the shared spin resolves. Do not change this to wallet or total balance unless the whole betting policy is deliberately revised.
- Blackjack stakes are deducted from bank when accepted. Double down and split each reserve one extra equal stake before changing the hand. Winners receive `stake * 2` back to bank, pushes receive `stake`, and losses receive nothing because the stake is already reserved. Do not add a Help button or player-vs-player flow to `/bets blackjack`.
- `/nick` rejects unauthorized staff emojis outright; do not reintroduce the old "send anyway" confirmation flow.
- Nickname emoji enforcement is event-driven for guild nickname updates, global-name updates, nickname resets, and member joins. Staff members from `Staff.isStaff(Member)`, emoji owners, explicitly authorized users, and unowned emojis are allowed. When sanitizing would leave an empty nickname, fallback replacements come from `Bot.getArray("nicks.replacements")`.

## Documentation
- Keep only this root `AGENTS.md`; do not add service-level copies.
- Update this file when changing mono-repo structure, shared workflows, service entrypoints, or feature ownership.
