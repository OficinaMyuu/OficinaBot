# AGENTS.md

## Purpose
This is the root index for agents working in the OficinaServices mono-repo. Keep this file as the single agent guide for the whole repository; do not add child `AGENTS.md` files.

## Layout
- `bot/` contains the Oficina Discord bot, formerly `OficinaMyuu/OficinaBot`.
- `backend/` contains the Go backend, formerly `OficinaMyuu/OficinaImagery`.
- `frontend/` contains the React/Vite web app deployed by Cloudflare Pages. The current authenticated dashboard lives at `/dashboard`.
- `backend/terraform/` contains the OCI Terraform source for backend infrastructure.
- `database/` contains the current bot MySQL migration runner and ordered migration SQL stream.
- `ansible.cfg` configures Ansible role and collection paths for repo-root commands.
- `ansible/` contains the Docker runtime provisioning and deployment playbooks for the OCI VMs.
- `registrar/` contains the registration Discord service, formerly `OficinaMyuu/RegistroOficina`.
- `.agents/skills/` contains project-local agent skills, currently `impeccable` and `vercel-react-best-practices`.
- `.github/workflows/` contains repo-level CI/deploy workflows. Keep workflows at the repository root so GitHub Actions can discover them.

## Hard Rules For Exploration
- Never read local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, anything inside `target/`, generated binaries, or local backups.
- Prefer service entrypoints and registration files before feature implementations.
- Keep changes scoped to the relevant service unless the task is explicitly cross-service.

## Service Entry Points
- Bot boot flow: `bot/src/main/java/ofc/bot/Main.java`
- Bot registration hub: `bot/src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
- Bot slash command registration: `bot/src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
- Product DB migrations: `database/cmd/migrator/main.go`; SQL files live under `database/migrations/`.
- Bot DB connection bootstrap: `bot/src/main/java/ofc/bot/domain/database/DB.java`
- Bot repository locator: `bot/src/main/java/ofc/bot/domain/database/repository/Repositories.java`
- Bot DB-backed config lookup: `bot/src/main/java/ofc/bot/internal/data/BotProperties.java`
- Backend entrypoint: `backend/cmd/api/main.go`; application setup lives under `backend/cmd/internal/app/`; Playwright-backed level card rendering lives under `backend/cmd/internal/service/`; HTTP handlers live under `backend/cmd/internal/http/handler/`; dashboard contracts live under `backend/cmd/internal/contract/`; dashboard entities live under `backend/cmd/internal/domain/entity/`; dashboard MySQL repositories live under `backend/cmd/internal/domain/mysql/repository/`; Discord API clients live under `backend/cmd/internal/infrastructure/discord/`.
- Frontend dashboard entrypoint: `frontend/src/main.tsx`; TanStack routes live under `frontend/src/routes/`; generated route metadata is `frontend/src/routeTree.gen.ts`; dashboard feature pages live under `frontend/src/pages/`; shared dashboard stores live under `frontend/src/stores/`.
- Backend Terraform entrypoint: `backend/terraform/`; the root module wires shared provider/backend/data concerns, while resources are split under `backend/terraform/modules/`.
- Ansible runtime entrypoint: `ansible/playbooks/site.yml`; example inventory lives under `ansible/inventories/example/`, and host setup/runtime roles live under `ansible/roles/`.
- Registrar entrypoint: `registrar/cmd/registrar/main.go`.

## Bot Project Snapshot
- Stack: Java 21, Maven, JDA 6, MySQL, jOOQ, HikariCP, Quartz, and OkHttp.
- App type: Discord bot for one server/community.
- Packaging: shaded jar built as `bot/target/bot.jar`.
- Runtime config is partly database-backed, not `.env`-driven.
- Secrets/config are fetched through `Bot.getSafe(...)` and `BotProperties`, which query the shared MySQL `config` table.
- Sad Monday/Sunday image posts are configured with `SAD_MONDAY_URL` and `SAD_SUNDAY_URL` environment variables.
- The DB schema is migration-first: product DDL lives in `database/migrations/`; Java table classes are query mappings only.
- The shared `users.avatar_hash` column is nullable and updated by normal bot user upserts plus avatar update events; do not backfill from Discord by bulk-loading guild members.
- The shared `users.is_bot` column is non-null, defaults to false, and is refreshed from Discord's `User.isBot()` by normal bot user upserts; do not backfill it by bulk-loading guild members.
- Do not add automatic migration logic or `CREATE TABLE IF NOT EXISTS` startup DDL to application services.
- Do not add interactive console SQL/query handlers to bot startup; database access should go through typed repositories, migrations, or purpose-built admin tooling.
- Many features are registered centrally, so missing behavior is often a registration problem, not a logic problem.

## Registrar Project Snapshot
- Stack: Go, `discordgo`, MySQL through `database/sql`, and the Go MySQL driver.
- App type: focused Discord registration service for the legacy `r!` registry flow.
- Entrypoint: `registrar/cmd/registrar/main.go`; application wiring lives under `registrar/internal/app/`.
- Runtime config uses `DATABASE_*` environment variables for MySQL connectivity, then reads `app.token`, `channels.registry`, and `channels.registry.log` from the shared `config` table.
- Legacy command routing lives in `registrar/internal/bot/Router`; `r!revoke` routes to revoke and every other `r!` message routes to the register command.
- Registration parsing and role IDs live in `registrar/internal/registration/`; keep the existing compact pattern syntax where gender is the first character, age is the digits, and device is the last character.
- The registry janitor deletes non-staff messages without digits in `channels.registry`, and removes recent registry-channel messages from users who leave the guild.
- Registrar uses Discord REST calls and short role caching instead of a large member cache; keep it light because it is intended to run on constrained OCI shapes.
- Registrar schema writes go through `registrar/internal/store/RegisterRepository` and the existing `registers` table. Do not add DDL to Registrar startup.

## Dashboard Project Snapshot
- Stack: React 19, Vite, TypeScript, TanStack Router, TanStack Query, Zustand, react-i18next, and react-icons.
- App type: authenticated operational dashboard served by Cloudflare Pages at `/dashboard`.
- Discord OAuth uses the existing application with `identify guilds` scopes. The callback is handled by the API at `/auth/discord/callback`.
- Dashboard access is restricted to the configured `DISCORD_GUILD_ID` when Discord reports guild owner, `Administrator`, or `Manage Server`.
- Dashboard modules include Birthdays, backed by the existing `birthdays` table; Tickets, backed by existing `support_tickets`, `messages_versions`, and `users` rows; and Economy action-cost configuration, backed by `store_item_settings`. Add schema changes through `database/migrations/`; the dashboard must not create tables at startup.
- Dashboard sessions are persisted in `dashboard_sessions`, keyed by a hash of the HttpOnly session cookie. Sessions should survive backend process restarts until their expiry.
- Dashboard API JSON fields use `snake_case`, including existing auth/session and birthday payloads. Batched user responses include the non-null `is_bot` flag. Discord snowflake IDs and all `created_at`/`updated_at` values are serialized as strings; timestamps are RFC 3339 UTC.
- Ticket and channel-message API responses carry user ID fields such as `initiator_id`, `closed_by_id`, `author_id`, and `deleted_by_id`; do not reintroduce embedded user objects. Dashboard clients should batch lookup display data through `POST /users/query`. Tickets load transcripts through the generic authenticated `GET /channels/{channel_id}/messages` API using the ticket payload's `channel_id`; do not restore ticket-specific message routes. Channel history supports mutually exclusive `before`, `after`, and `around` message-ID anchors and returns chronological pages with directional availability flags.
- Mutating dashboard API requests use the session CSRF token returned by `/auth/me` in `X-CSRF-Token`.

## Bot Directory Index
- `bot/src/main/java/ofc/bot/commands/`: slash command implementations by feature area.
- `bot/src/main/java/ofc/bot/listeners/`: JDA event listeners, split into guild/log/interaction/moderation/economy areas.
- `bot/src/main/java/ofc/bot/jobs/`: Quartz scheduled jobs and recurring automation.
- `bot/src/main/java/ofc/bot/handlers/`: framework glue, registries, interaction gateways, pagination, moderation, games, groups, economy.
- `bot/src/main/java/ofc/bot/domain/`: entities, enums, tables, view models, MySQL bootstrap, repositories.
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
- Change persistence behavior: open the related table/entity/repository trio under `bot/src/main/java/ofc/bot/domain/`.
- Change schema: add an ordered migration under `database/migrations/`; applications must not own DDL.
- Debug config or startup failures: start with `Main.java`, `BotProperties.java`, and `DB.java`.
- Debug command visibility/registration: start with `CommandsInitializer.java` and `SlashCommandsRegistryManager.java`.

## Bot Feature Map
- Economy: `commands/impl/slash/economy/`, `listeners/discord/economy/`, `handlers/economy/`, `UserEconomyRepository`. `/rob` steals wallet only and fines bank on failure. `PolicyType.BLOCK_MONEY_GAINS` blocks automated money earnings only. Purchasable group actions, color roles, counting-punishment releases, and marriage acceptance are seeded in `store_item_settings` and resolved by `StoreItemSettingsRepository`; `StoreItemType` stays a static type key and missing settings must fail closed rather than revive hard-coded prices. Color roles use the one global `COLOR_ROLE` price, never a per-role value.
- Color roles: `/colors` Components V2 store lives in `commands/impl/slash/colors/`; rendering lives in `handlers/shop/ColorRoleStoreMessageFactory.java`; final buy/remove actions stay in the shop button handlers.
- Accumulator prizes: `/accumulator add/import/list` live in `commands/impl/slash/accumulator/`; money prizes default to the UnbelievaBoat economy; import reads newline-separated user IDs from a same-channel message, validates members through batched Discord gateway lookups instead of cache-only checks, and can forbid duplicates against both the import payload and pending prizes. Durable list controls live in `listeners/discord/interactions/buttons/accumulator/`; rendering, import planning, member resolution, and payout orchestration live in `handlers/accumulator/`; rows live in `accumulator_prizes`.
- Groups: `commands/impl/slash/groups/`, `listeners/discord/interactions/buttons/groups/`, `handlers/groups/`, `OficinaGroupRepository`. Group emojis are unique across groups and must use binary MySQL collation when persisted. Role emoji display is stored on `groups.has_role_emoji`; when enabled the role name is `{emoji}⠀⠀⠀⠀{name}⠀⠀⠀⠀{emoji}`, otherwise it is `⠀⠀⠀⠀⠀⠀{name}⠀⠀⠀⠀⠀⠀`. `/group info` uses Discord role member counts, must not chunk/load all guild members, and shows rent as disabled while member-intersection-based rent is unavailable.
- Marriage/relationships: `commands/impl/slash/relationships/`, `MarriageRepository`, `MarriageRequestRepository`. Non-privileged marriage acceptance charges each participant the configured `MARRIAGE` wallet price.
- Userinfo: `commands/impl/slash/userinfo/`, with the counting punishment release buttons under `listeners/discord/interactions/buttons/userinfo/`; the release role id is read from `fun.counting.punishments.role.id` and purchases charge the selected provider's bank using the configured `COUNTING_RELEASE` price. The release button is shown only when a member views their own `/userinfo` and currently has the configured role.
  Member join history is stored in `member_join_events`; `/userinfo` shows the earliest known join event and falls back to JDA's current member join timestamp when no row exists yet. Live joins are recorded by `MemberJoinUpsert`.
- Reminders: `commands/impl/slash/reminders/`, `jobs/RemindersHandler.java`, `ReminderRepository`.
- Moderation: `commands/impl/slash/moderation/`, `listeners/discord/moderation/`, `handlers/moderation/`, punishment repositories.
- Events: `ToggleEventsCommand.java`, configured by `channels.events.text.id` and `channels.events.voice.id`.
- Role member lookup: `commands/impl/slash/RoleMembersCommand.java`; `/rolemembers` keeps the response syntax as aligned `id -> username` rows and sorts rows alphabetically by Discord username before sending inline text or the large-result file fallback.
- Role info: `commands/impl/slash/RoleInfoCommand.java`; `/roleinfo` uses Discord role member counts and must not chunk/load all guild members or show online-by-role counts.
- Attachment forwarding log: `listeners/discord/logs/messages/AttachmentForwardingLogger.java`; user-sent guild messages with attachments are forwarded to the text channel configured by `channels.attachments-log.id` through Discord's native message forward action.
- World Cup 2026 reaction role: `listeners/discord/guilds/reactionroles/WorldCup2026ReactionRoleHandler.java`; messages in `worldcup2026.channel_id` receive a soccer ball reaction, and users who add/remove that reaction are idempotently granted/removed from `worldcup2026.role_id`.
- Levels/XP: `commands/impl/slash/levels/`, `UsersXPHandler.java`, `VoiceXPHandler.java`, `LevelManager.java`. Voice XP channel payout overrides live in `voice_channel_income_rules` with `payout_type = LEVEL_EXPERIENCE`.
  Rank and level-role card rendering posts to the backend card API configured by `backend.api.base-url`, for example `http://10.0.1.10:8080`; the bot appends `/levels/cards` or `/levels/roles` and expects successful responses as raw `image/png` bytes.
- Automated money income: `ChatMoneyHandler.java`, `VoiceChatMoneyHandler.java`, and `AutomatedMoneyGainPolicy.java`. Voice channels listed in `income.voice.bank-channel-ids` pay UnbelievaBoat income to bank with the voice multiplier instead of cash. Voice money channel payout overrides live in `voice_channel_income_rules` with `payout_type = MONEY`.
- Tickets: `commands/impl/slash/tickets/`, modal/button handlers under `listeners/discord/interactions/.../tickets/`.
  Initial ticket messages expose durable add/remove member buttons plus close; member add/remove handling lives in `listeners/discord/interactions/buttons/tickets/TicketMemberManagementHandler.java`. Managing ticket members is restricted to users with `Manage Server` or Support Superior-or-higher staff roles. `/tickets view` relies on indexed ticket ordering plus `messages_versions(channel_id, author_id)` for participant lookup.
- Mafia/bets/games: `commands/impl/slash/mafia/`, `commands/impl/slash/bets/`, `handlers/games/`. `/bets roulette` uses a timed channel lobby, bank-only stakes, and UnbelievaBoat-style roulette spaces under `handlers/games/betting/roulette/`. A user may have only one active roulette entry per lobby; repeating the command replaces that user's previous entry. `/bets blackjack` is single-player against the bot/dealer under `handlers/games/betting/blackjack/`; it is not player-vs-player.
- Giveaways: `/giveaway create/end/reroll`, `handlers/giveaway/`, `GiveawayInteractionListener.java`, `GiveawayVoiceConditionListener.java`, `GiveawayEndHandler.java`, and the `giveaways`, `giveaway_entries`, and `giveaway_winners` tables.
- Oficina Dorme internals: `handlers/games/mafia/service/`, `handlers/games/mafia/domain/`, `handlers/games/mafia/discord/`, `MafiaInteractionListener.java`, `MafiaLifecycleListener.java`, and `game_mafia_logs`.
- Nickname changes: `NickCommand.java`, `handlers/nick/`, `NicknameUpdateRequestGuard.java`, `listeners/discord/guilds/members/*NicknameEmojiEnforcementListener.java`, `listeners/discord/interactions/buttons/nick/`, and `nickname_update_requests`.
- Generic throttled updates: `handlers/ThrottledAction.java`.

## Builds And Tests
- Bot tests: run `mvn "-Dmaven.repo.local=../.m2" test` from `bot/`.
- Bot DB integration tests use live MySQL when `OFICINA_TEST_MYSQL_JDBC_URL` is set. The Java helper creates a temporary schema per test and drops it on close; otherwise DB tests are skipped.
- Bot package: run `mvn clean package` from `bot/`.
- Bot container image: run `docker build -t oficina-bot ./bot` from the repository root. The image runs as UID/GID `10001` with writable runtime state under `/var/lib/oficina/bot`.
- Bot and registrar database config comes from `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, and `DATABASE_PASSWORD`. Registrar maps `DATABASE_MAX_POOL_SIZE`, `DATABASE_MIN_IDLE`, `DATABASE_CONNECTION_TIMEOUT_MS`, `DATABASE_VALIDATION_TIMEOUT_MS`, `DATABASE_IDLE_TIMEOUT_MS`, and `DATABASE_MAX_LIFETIME_MS` onto `database/sql`; the Java bot also supports the Hikari-specific `DATABASE_KEEPALIVE_TIME_MS` knob.
- Registrar tests/build: run `go test ./...` and `go build ./...` from `registrar/`.
- Registrar DB integration tests use live MySQL when `OFICINA_TEST_MYSQL_DSN` is set; otherwise the live DB test is skipped.
- Registrar container image: run `docker build -t oficina-registrar ./registrar` from the repository root. The image builds with Go 1.25 to match `registrar/go.mod`, runs as UID/GID `10001`, and keeps writable runtime state under `/var/lib/oficina/registrar`.
- Backend tests: run `go test ./...` from `backend/cmd/`. Set `OFICINA_TEST_MYSQL_DSN` to run live dashboard repository tests against a temporary schema. The backend module currently requires Go 1.25; keep `backend/Dockerfile` on a matching Go 1.25 builder image.
- Frontend dashboard tests/build: run `npm test`, `npm run lint`, and `npm run build` from `frontend/`.
- Database migrator tests/build: run `go test ./...` from `database/`.
- Run product migrations with `go run ./cmd/migrator up` from `database/` after setting `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, and `DATABASE_PASSWORD` for a DDL-capable migration user.
- Backend Terraform validation: run `terraform fmt -check -recursive`, `terraform init -backend=false`, and `terraform validate` from `backend/terraform/`.
- Backend DB integration tests are pending redesign because the current `database/migrations` stream is bot-only.
- Ansible validation: run `ansible-galaxy collection install -r ansible/requirements.yml`, then `ansible-playbook -i ansible/inventories/example/hosts.yml ansible/playbooks/site.yml --syntax-check` from the repository root. Use a private inventory for real hosts.
- For doc-only changes, a file review is enough.

## Deployments
- Bot image workflow: `.github/workflows/deploy.yml`; pushes `ghcr.io/<owner>/oficina-bot:latest` and `ghcr.io/<owner>/oficina-bot:<sha>`.
- Registrar image workflow: `.github/workflows/deploy-registrar.yml`; pushes `ghcr.io/<owner>/oficina-registrar:latest` and `ghcr.io/<owner>/oficina-registrar:<sha>`.
- Backend image workflow: `.github/workflows/deploy-backend.yml`; builds from the repository root with `backend/Dockerfile` using a Go 1.25 builder, then pushes `ghcr.io/<owner>/oficina-backend:latest` and `ghcr.io/<owner>/oficina-backend:<sha>`.
- Bot and registrar Dockerfiles are service-local. The bot builds a shaded Maven jar into an Eclipse Temurin Alpine JRE runtime. Registrar builds a static Go binary into an Alpine runtime. Both run through `dumb-init` as the non-root `app` user.
- CodeQL workflow: `.github/workflows/codeql.yml`; scans Java/Kotlin and Go with explicit monorepo build steps.
- Runtime deployment is managed by Ansible from `ansible/`. The bots VM runs the `bot`, any additional bot containers defined in inventory, and `registrar`; the backend/API VM runs the `backend` container.
- All application containers are deployed through host-level Docker Compose projects generated by Ansible. The bots stack contains `bot`, `registrar`, and `watchtower`; the backend stack contains `backend` and `watchtower`.
- Generated Compose files must load service env files with `env_file.format: raw`; service secrets can contain `$`, and normal Compose interpolation would otherwise mutate those values before containers receive them.
- Watchtower is configured to poll every 300 seconds and update all running containers on each host. It pins `DOCKER_API_VERSION` through `oficina_watchtower_docker_api_version` so newer Docker daemons do not reject Watchtower's default legacy client API. Do not rely on Watchtower labels unless the host starts running non-Oficina containers.
- Ansible connects directly to both VMs as `ubuntu` through their public IPs or DNS names. SSH ingress is restricted by `ssh_source_cidr`; keep the private SSH key on the operator machine.
- The VMs do not need `git` for deployment. They pull images from GHCR, so they need Docker, DNS, outbound HTTPS, and GHCR credentials only when packages are private.
- Backend Terraform workflow: `.github/workflows/backend-terraform.yml`; pull requests validate without secrets, pushes to `main` plan against the OCI backend, and applies require manual dispatch with the `backend-infra` environment.
- Backend Terraform state backend values are not committed. Use an ignored `backend.oci.tfbackend` locally, and GitHub secrets `OCI_OBJECT_STORAGE_NAMESPACE` and `OCI_TF_STATE_BUCKET` in CI.
- Backend Terraform is constrained for OCI Always Free: the API VM uses `VM.Standard.E2.1.Micro`, the bots VM uses `VM.Standard.A1.Flex` with 1 OCPU and 6 GB RAM, the load balancer is fixed at 10 Mbps, MySQL uses `MySQL.Free` with 50 GB storage, and compute boot volumes default to 50 GB.
- Backend Terraform currently exposes the OCI load balancer as public IPv4 HTTP-only. SSH to both application VMs is allowed from `ssh_source_cidr` for direct admin and Ansible access. Add HTTPS later through Cloudflare DNS/proxying, Cloudflare Origin CA material on the OCI load balancer, and a 443 listener.
- Backend Terraform allows the bots NSG to reach the API NSG on `api_port` so bot commands can call the backend private address configured in `backend.api.base-url`.
- Persistence uses the provisioned MySQL DB system. Terraform provisions infrastructure only; schema changes are applied by the separate `database/` migrator. Use a DDL-capable migration user for the migrator and restricted application users for runtime services.
- Backend app APIs include Playwright-backed `POST /levels/cards`, `POST /levels/roles`, compatibility aliases under `/api/levels/*`, static template assets, unauthenticated `GET /health`, OAuth under `/auth/*`, authenticated birthday CRUD under `/birthdays`, authenticated user lookup under `/users/query`, authenticated ticket reads under `/tickets`, generic authenticated channel-message reads and version history under `/channels/{channel_id}/messages`, authenticated Discord Lottie sticker assets under `/discord/stickers/{sticker_id}/lottie`, and authenticated action-cost reads/updates under `/economy/action-costs`. The backend does not serve frontend routes or assets.
- Backend liveness is exposed by unauthenticated `GET /health`; the Docker image and compose file use this endpoint for container health checks. Build the image from the repository root with `docker build -f backend/Dockerfile -t oficina-backend .`.
- Backend Compose mounts `./static` next to the generated compose file into `/app/static:ro`. The Ansible runtime role copies `backend/static/` through `oficina_compose_assets`; keep this in sync when backend templates or assets move.
- Backend Compose must run the API service with `ipc: host` because Chromium/Playwright can otherwise crash during startup inside Docker's default small shared-memory namespace.
- Backend Playwright startup expects the Playwright Go driver to be pre-baked into `/var/lib/oficina/backend/playwright-driver` and Playwright-managed Chromium to be pre-baked into `/var/lib/oficina/backend/ms-playwright` by `backend/Dockerfile`. The backend uses the maintained `github.com/mxschmitt/playwright-go` module, whose driver installer no longer depends on the deprecated Playwright driver CDN. Application startup must only verify and run those baked artifacts; do not call `playwright.Install` at runtime or let production download into `/home/appuser`/cache paths.
- Backend CORS allows configured frontend origins with credentials enabled, and body limit defaults to `BODY_LIMIT=8M`.
- Backend dashboard runtime needs `PUBLIC_API_BASE_URL`, `FRONTEND_BASE_URL`, optional `CORS_ALLOWED_ORIGINS`, `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_GUILD_ID`, and `DATABASE_*` in the backend environment. The Discord Developer Portal callback is `${PUBLIC_API_BASE_URL}/auth/discord/callback`, for example `https://api.oficinamyuu.com.br/auth/discord/callback`.
- Production browser access expects Cloudflare Pages to serve `https://oficinamyuu.com.br/dashboard`; do not proxy dashboard UI paths to the Go backend.
- Private Ansible inventories, registry tokens, and service environment values must stay out of git. Use `ansible/inventories/prod/` or Ansible Vault for real values.

## Known Traps
- Do not assume env files exist; bot config is often loaded from the DB `config` table.
- The `config` table column is named `key`; always quote it in SQL/jOOQ lookups because current MySQL versions treat it as reserved syntax.
- Voice income bank-channel overrides are configured through `income.voice.bank-channel-ids` as semicolon-separated Discord channel IDs; do not hard-code channel snowflakes in `VoiceChatMoneyHandler`.
- Voice channel income customizations are database-backed in `voice_channel_income_rules`, keyed by `(channel_id, payout_type)`. Use `multiplier = 1.25`, `allow_muted = true`, and `allow_solo = true` for event channels that should pay 125% to muted or solo undeafened humans.
- Coinflip inference channel bans are configured through `messages.coinflip.banned-channel-ids` as Discord channel IDs returned by `Bot.getArray(...)`; banned channels are ignored before pending flips or cooldowns are updated.
- Attachment forwarding uses `channels.attachments-log.id` as a Discord text channel ID. It is stateless: do not add a table, scan archive history, or re-upload attachment bytes unless the preservation policy is deliberately changed.
- Do not assume a missing bot feature is unimplemented before checking central registration.
- Applications must not run schema creation or migrations at startup. Java `DB.java` files and Go database packages only configure MySQL connections and verify connectivity.
- Giveaway buttons are durable component ids prefixed with `giveaway:` and must not use `InteractionMemoryManager`.
- Accumulator controls are durable component ids prefixed with `acc:v1:` and must not use `InteractionMemoryManager`; accumulator rows are never deleted, only moved from `PENDING` to `PAID` or `REJECTED`.
- Color role ownership uses `color_roles_state.expires_at`; do not reintroduce fixed `updated_at + 60 days` expiration logic.
- `ColorRoleRemotionHandler` deletes stale `color_roles_state` rows when an expired row points at a Discord role that no longer exists.
- Roulette stakes are deducted from bank when accepted, and replacing an active entry releases the previous reserved stake before reserving the new one. Winners receive `stake * multiplier` back to bank when the shared spin resolves. Do not change this to wallet or total balance unless the whole betting policy is deliberately revised.
- Blackjack stakes are deducted from bank when accepted. Double down and split each reserve one extra equal stake before changing the hand. Winners receive `stake * 2` back to bank, pushes receive `stake`, and losses receive nothing because the stake is already reserved. Do not add a Help button or player-vs-player flow to `/bets blackjack`.
- `/nick` rejects unauthorized staff emojis outright; do not reintroduce the old "send anyway" confirmation flow.
- Emoji columns used as identifiers or permission keys must use `utf8mb4_bin`, not the schema default `utf8mb4_unicode_ci`, because MySQL linguistic collations can compare unrelated emoji as equal.
- Nickname emoji enforcement is event-driven for guild nickname updates, global-name updates, nickname resets, and member joins. Staff members from `Staff.isStaff(Member)`, emoji owners, explicitly authorized users, and unowned emojis are allowed. When sanitizing would leave an empty nickname, fallback replacements come from `Bot.getArray("nicks.replacements")`.

## Documentation
- Keep only this root `AGENTS.md`; do not add service-level copies.
- Update this file when changing mono-repo structure, shared workflows, service entrypoints, or feature ownership.
