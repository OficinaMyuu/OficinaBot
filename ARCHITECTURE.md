# ARCHITECTURE.md

## Overview
OficinaServices is the mono-repo for Oficina's Discord-facing services and shared backend work. Keep this file as the single architecture guide for the repository; do not add child `ARCHITECTURE.md` files.

## Services
- `bot/`: Java 21 Discord bot using JDA 6, Maven, MySQL, jOOQ, HikariCP, Quartz, and OkHttp.
- `backend/`: Go HTTP backend for Playwright-backed level card image generation. Its current entrypoint is `backend/cmd/api/main.go`, with server bootstrapping under `backend/cmd/internal/app/`.
- `registrar/`: Java 17 Discord registration service using JDA 5 and Maven. Its entrypoint is `registrar/src/main/java/ofc/bot/RegisterMaster.java`.

## Repository Structure
- Repo-level GitHub Actions workflows live in `.github/workflows/`.
- Service source, build descriptors, and service-owned assets live inside each service directory.
- Runtime files, generated artifacts, local databases, and package outputs are ignored and are not source of truth.
- Bot database migrations live in `database/`; service code must not create or alter schema at startup.
- Backend infrastructure source lives in `backend/terraform/`.
- Docker host provisioning and runtime deployment source lives in `ansible/`.

## Deployment Model
- The bot workflow builds and pushes `ghcr.io/<owner>/oficina-bot:latest` and a SHA-tagged image from `bot/Dockerfile`.
- The registrar workflow builds and pushes `ghcr.io/<owner>/oficina-registrar:latest` and a SHA-tagged image from `registrar/Dockerfile`.
- The backend workflow builds and pushes `ghcr.io/<owner>/oficina-backend:latest` and a SHA-tagged image from `backend/Dockerfile`.
- The bot container image is built from `bot/Dockerfile`. It uses a Maven/Java 21 builder stage, an Eclipse Temurin Java 21 Alpine JRE runtime, and runs as UID/GID `10001` with writable state under `/var/lib/oficina/bot`.
- The registrar container image is built from `registrar/Dockerfile`. It uses a Maven/Java 17 builder stage, an Eclipse Temurin Java 17 Alpine JRE runtime, and runs as UID/GID `10001` with writable state under `/var/lib/oficina/registrar`.
- Bot and registrar containers keep immutable jars under `/opt/oficina` and connect to the shared OCI MySQL database through `DATABASE_*` environment variables. Writable state remains available for logs and other runtime files, but database state is no longer a container-local SQLite file.
- Ansible manages runtime deployment through Docker Compose on the OCI VMs. The bots VM runs `bot`, any additional bot containers defined in inventory, `registrar`, and Watchtower. The backend/API VM runs `backend` and Watchtower.
- Watchtower polls every 300 seconds and updates all containers on each host. The Compose template sets `DOCKER_API_VERSION` from `oficina_watchtower_docker_api_version` because newer Docker daemons reject Watchtower's legacy default API version. GHCR credentials are required on the hosts only when packages are private.
- The backend Terraform workflow validates infrastructure changes on pull requests, plans against the remote OCI backend on pushes to `main`, and applies only through manual dispatch with the `backend-infra` GitHub Environment.

## Runtime Operations
The Ansible entrypoint is `ansible/playbooks/site.yml`. It installs Docker Engine and the Compose plugin, configures Docker log rotation, renders host-level Compose projects, writes service environment files from inventory variables, logs in to GHCR when credentials are provided, and starts the stacks.

The production topology uses the `ubuntu` SSH user. Ansible reaches both VMs directly through their public IPs or DNS names, with SSH restricted to the configured `ssh_source_cidr`. The local operator machine keeps the private key. Terraform injects the same configured public SSH key into both compute instances.

The VMs do not need `git` for deployment because GitHub Actions builds the images and pushes them to GHCR. They do need DNS and outbound HTTPS access for Docker and Watchtower pulls. With the current OCI layout, the application VMs are in the public apps subnet with internet-gateway egress and SSH protection through network security groups. If the backend VM is later moved behind a bastion or into a subnet without direct egress, update the Ansible inventory for `ProxyJump` and add NAT or a registry mirror before relying on Watchtower updates.

## Backend Infrastructure
Backend OCI infrastructure is managed from `backend/terraform/`. The root Terraform module owns provider/backend setup, shared data sources, common tags, module wiring, and outputs. Resource ownership is split by concern under `backend/terraform/modules/`: `network` owns VCN/subnets/routing/security, `compute` owns the API and bot instances, `mysql` owns the private DB system, and `load_balancer` owns the public flexible load balancer.

The OCI Terraform backend uses partial configuration in source (`backend "oci" {}`), with the real bucket, namespace, region, and state key supplied through an ignored local `backend.oci.tfbackend` file or generated from GitHub secrets in CI. The OCI API private key is represented as `private_key_path` in Terraform; the GitHub Actions helper script writes `OCI_PRIVATE_KEY_PEM` into a temporary PEM file and exports `TF_VAR_private_key_path` to keep local and CI provider configuration equivalent.

The backend infrastructure is constrained to OCI Always Free shapes and sizes: the API VM uses `VM.Standard.E2.1.Micro`, the bots VM uses `VM.Standard.A1.Flex` with 1 OCPU and 6 GB RAM, the load balancer is fixed at 10 Mbps, MySQL uses `MySQL.Free` with 50 GB storage, and compute boot volumes default to 50 GB. The current load balancer is public IPv4 HTTP-only for initial reachability and smoke tests. SSH to both application VMs is allowed only from the configured admin CIDR. Backend HTTP reaches the API VM from the load balancer security group, and the bots NSG may also reach the API port privately for bot-owned card rendering calls. Production HTTPS should be added later through Cloudflare DNS/proxying, a Cloudflare Origin CA certificate installed on the OCI load balancer, a 443 listener, and Cloudflare Full (strict) SSL/TLS mode.

## Database Schema Management
Terraform owns the OCI MySQL DB system, networking, and compute plumbing only. The current bot schema is owned by the root `database/` module, which embeds ordered goose SQL migrations and exposes `database/cmd/migrator`. Run the migrator as a separate deployment step with a DDL-capable migration user:

```powershell
go run ./cmd/migrator up
```

The main bot connects to this schema after migrations have run. Runtime application users should be restricted to the DML privileges they need; MySQL admin or migration credentials should not be placed on application VMs. Backend/dashboard persistence is intentionally pending redesign and is not represented in the current migration stream.

## Bot Boot Flow
1. `DB.init()` creates the MySQL/Hikari datasource and verifies connectivity.
2. JDA is built and awaited.
3. Console handler and Quartz jobs are initialized.
4. Services, listeners, slash commands, and composed interactions are registered.

## Bot Architectural Pieces
- Entry point: `bot/src/main/java/ofc/bot/Main.java`
- Application wiring: `bot/src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
- Slash command registration: `bot/src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
- Persistence bootstrap: `bot/src/main/java/ofc/bot/domain/database/DB.java`
- Repository access: `bot/src/main/java/ofc/bot/domain/database/repository/Repositories.java`
- Shared utility helpers: `bot/src/main/java/ofc/bot/util/`

## Bot Persistence Shape
- The bot connects to the shared MySQL schema through `DATABASE_HOST`, `DATABASE_PORT`, `DATABASE_NAME`, `DATABASE_USER`, and `DATABASE_PASSWORD`.
- The bot no longer depends on runtime `content/` or `assets/` directories. Scheduled Sad Monday/Sunday image posts read URL strings from `SAD_MONDAY_URL` and `SAD_SUNDAY_URL`.
- Table definitions live under `bot/src/main/java/ofc/bot/domain/tables/`
- Entity models live under `bot/src/main/java/ofc/bot/domain/entity/`
- Repository implementations live under `bot/src/main/java/ofc/bot/domain/database/repository/`
- Runtime configuration is partially stored in the MySQL `config` table and accessed via `BotProperties`.
- Schema migrations are performed by the root `database/` migrator. Bot startup never runs `CREATE TABLE IF NOT EXISTS`.
- `voice_channel_income_rules` customizes scheduled voice money and XP payouts per channel and payout type.
- `member_join_events` stores every known guild join event per user. `/userinfo` reads the earliest stored event and falls back to Discord's current member join timestamp when no history exists.
- `groups.has_role_emoji` controls whether a group role name includes the group's emoji around the display name. Group channels always use the group emoji, while role names use either six braille-blank spacers without emoji or four braille-blank spacers between emoji and name when the flag is enabled.
- Emoji values that are used as identifiers, such as group emoji ownership and nickname emoji permissions, are stored as `utf8mb4` strings with `utf8mb4_bin` column collation. Do not use the schema default `utf8mb4_unicode_ci` for these columns, because MySQL linguistic collations can compare distinct emoji as equal and break unique indexes or exact permission checks.

Legacy SQLite-era manual snippets are no longer source of truth; add future changes as ordered MySQL migrations under `database/migrations/`.

## Bot Interaction Model
- Slash commands live under `bot/src/main/java/ofc/bot/commands/impl/slash/`.
- JDA event listeners live under `bot/src/main/java/ofc/bot/listeners/`.
- Button/modal/menu/autocomplete routing is centralized under `bot/src/main/java/ofc/bot/handlers/interactions/`.
- Scheduled jobs live under `bot/src/main/java/ofc/bot/jobs/`.
- `StaffFormReminder` wakes through Quartz every Saturday at 14:00 and applies an odd/even continuous-week guard so reminders are sent every other Saturday.

`/rolemembers` resolves role members through JDA, reports the same online/offline/total statistics for the unsorted role membership set, and renders aligned `id -> username` rows sorted alphabetically by Discord username. The same ordering is used when large responses are sent as `members.txt`.

`/roleinfo` and `/group info` use Discord role member counts instead of JDA member chunking. They must not load all guild members just to display total role/group membership. `/roleinfo` does not show online-by-role counts, and `/group info` shows rent as disabled until rent can be computed without hydrating all role members.

Coinflip message inference is handled by `CoinflipInferenceHandler` for plain `cara`/`coroa` messages. Channels listed in `messages.coinflip.banned-channel-ids` are ignored before pending flips or cooldowns are touched. The key is read with `Bot.getArray(...)` and stores raw Discord channel IDs.

Attachment preservation is handled by `AttachmentForwardingLogger`. When a non-bot, non-webhook guild message contains attachments, the bot forwards that message with JDA's native `Message#forwardTo(...)` API to the text channel configured by `channels.attachments-log.id`. The feature is intentionally stateless: it does not create a database table, scan the archive channel, or download/re-upload attachment bytes.

## Bot Tickets
Ticket creation sends a durable initial message with add-member, remove-member, and close controls. Close opens the existing close-reason modal. Add/remove member controls are handled by durable component IDs in `TicketMemberManagementHandler` instead of the temporary interaction memory manager, because ticket messages can outlive a bot process restart. Only members with `Manage Server` or Support Superior-or-higher staff roles may use those member controls. Adding skips users that already have ticket access. Removing deletes only explicit member permission overrides, skips the ticket initiator, and ignores administrators.

`/tickets view` renders one ticket at a time and derives the involved users from `messages_versions` with a distinct author lookup for the ticket channel. Keep `messages_versions(channel_id, author_id)`, `support_tickets(created_at)`, and `support_tickets(initiator_id, created_at)` indexed so ticket pagination and participant lookup stay bounded as archived messages and tickets grow. Apply ticket index changes through the `database/` migrator, for example `go run ./cmd/migrator up` from `database/` with the `DATABASE_*` variables set for a DDL-capable MySQL user.

## Bot Economy
Automated income is split by economy provider: `ChatMoneyHandler` credits Oficina wallet money for eligible guild messages, while `VoiceChatMoneyHandler` credits UnbelievaBoat cash or bank money for eligible voice activity. Voice channels configured in `income.voice.bank-channel-ids` are semicolon-separated Discord channel IDs that pay the doubled voice income amount to bank instead of cash. Both paths honor `PolicyType.BLOCK_MONEY_GAINS` through `AutomatedMoneyGainPolicy`, matching blocked users, roles, or channels. The policy is intentionally limited to automated income and does not block explicit command rewards such as `/daily` and `/work`, nor manual or claim-based prize fulfillment such as giveaway money claims.

`voice_channel_income_rules` stores per-channel overrides for scheduled voice payouts. Rows are keyed by `(channel_id, payout_type)`, with payout types `MONEY` and `LEVEL_EXPERIENCE`. `multiplier` scales the final payout after normal channel routing, so a bank voice money channel with multiplier `1.25` pays 125% of the doubled bank amount. `allow_muted` lets muted, undeafened humans receive that payout type, and `allow_solo` removes the two-human minimum for that payout type. Bots and deafened members are always excluded. The jobs load matching rows once per scheduled run and use default behavior when no row exists.

An event voice channel that should pay 125% money and XP to muted or solo undeafened humans needs one row per payout type:

```sql
INSERT INTO voice_channel_income_rules
    (guild_id, channel_id, payout_type, multiplier, allow_muted, allow_solo, created_by, created_at, updated_at)
VALUES
    (:guild_id, :channel_id, 'MONEY', 1.25, true, true, :staff_user_id, :unix_now, :unix_now),
    (:guild_id, :channel_id, 'LEVEL_EXPERIENCE', 1.25, true, true, :staff_user_id, :unix_now, :unix_now);
```

UnbelievaBoat access is intentionally isolated behind `UnbelievaBoatClient` and `UnbelievaBoatRequester`. The client owns guild-scoped economy operations and sends the configured raw API token in the `Authorization` header, matching UnbelievaBoat's documented auth format. The requester owns HTTP retry behavior: successful responses may pause when `X-RateLimit-Remaining` is nearly exhausted, while HTTP 429 responses retry with the JSON `retry_after` delay first, then `X-RateLimit-Reset`, then local exponential backoff.

`/rob` only steals from the target user's wallet. The failure probability follows the UnbelievaBoat-style formula `robber net worth / (target wallet + robber net worth)`, clamped to `20%` through `80%`. On success, the stolen amount is the success probability multiplied by the target wallet and rounded up. On failure, the robber is fined using the UnbelievaBoat crime default range of `20%` through `40%` of their net worth. The fine is applied to bank balance, because wallet cannot be negative while bank balance is allowed to represent debt.

`/bets roulette` is a timed, channel-scoped betting lobby modeled after UnbelievaBoat roulette. The first accepted bet opens a 30-second shared spin; other users, and the same user, can place more bets in that lobby before the ball lands. Supported spaces are red/black, even/odd, 1-18/19-36, 1st/2nd/3rd columns, 1-12/13-24/25-36 dozens, and exact numbers 0 through 36. Stakes are deducted from bank immediately and accepted amounts range from 100 to 1,000,000. Stake input uses the shared economy amount parser, so shorthand such as `all`, `2k`, and `750k` resolves against the bettor's bank before the roulette bounds are applied. Winners receive `stake * multiplier` back to bank on settlement, using x2, x3, or x36 according to the selected space. Settlement leaves the final lobby message visible and sends a separate result embed with winner and loser fields, omitting either field when that group is empty. Completed spins reuse the existing `bet_games` and `games_participants` tables with `GameType.ROULETTE`.

`/bets blackjack` is a single-player Blackjack game against the bot/dealer, modeled after UnbelievaBoat's public rules. It uses the same bank-only stake range as roulette, 100 through 1,000,000, and the same amount parser for values such as `all`, `2k`, and `750k`. The game uses a shuffled three-deck shoe, deals two player cards and two dealer cards, hides the dealer hole card until settlement, and renders all player-facing embed text and buttons in pt-BR. The active controls are `Pedir Carta`, `Parar`, `Dobrar`, and `Dividir`; card counts render in the embed footer as `Cartas restantes`; there is intentionally no Help button and no player-vs-player invite flow.

Blackjack stakes are reserved from bank immediately. Double down and split each reserve one additional equal stake before the hand changes; if the user no longer has enough bank, the action is rejected and the hand is re-rendered. Natural Blackjack is only the original unsplit two-card 21, beats non-natural 21, and pays even money. Wins credit `stake * 2` back to bank, pushes credit `stake`, and losses credit nothing because the stake has already been deducted. Split is allowed once, only for equal ranks, and split hands may still double down when they have exactly two cards and enough bank. Inactive games auto-stand after five minutes, settle, disable controls, and clear the active betting lock. Completed hands reuse `bet_games` and `games_participants` with `GameType.BLACKJACK`; no blackjack-specific table is required.

Blackjack card custom emoji names are defined in the `BlackjackCard` enum. Code identifiers and emoji names remain English: `bj_a_c` through `bj_k_c`, the same rank names with `_d`, `_h`, and `_s`, plus `bj_back`. The enum stores `emojiName` and `emojiId` separately and falls back to readable text while the real Discord emoji IDs are still `0L`.

## Bot Moderation
Automod warnings are persisted before the current threshold is resolved through `AutomodActionRepository`. When the configured threshold resolves to `KICK`, the Discord kick is queued first, and cleanup runs only after JDA reports that kick as successful. Cleanup deletes the user's XP row from `users_xp` and resets every configured economy account to zero, currently Oficina Bank and UnbelievaBoat.

## Bot Levels
`LevelManager` grants XP, persists level progress, announces level-ups in the configured level-up channel, and applies matching level roles. `VoiceXPHandler` uses `voice_channel_income_rules` rows with `payout_type = LEVEL_EXPERIENCE` to customize voice XP payout multipliers and event-channel eligibility. Users can run `.toggle-rankup-pings` to control whether their level-up announcement mentions them. The command is a guild legacy listener available to every user, stores the deterministic boolean value in `users_preferences.rankup_pings_enabled`, and preserves `users_preferences.locale`, which can remain null until Discord exposes it through an interaction.

`/rank` and `/levels-roles` render image cards through the backend card API. The bot reads `backend.api.base-url` from the shared `config` table and appends `/api/levels/cards` or `/api/levels/roles`; production should point this key at the backend private subnet address, such as `http://10.0.1.10:8080`. These backend endpoints are unauthenticated and do not use the legacy AWS API Gateway `x-api-key` header.

## Bot Channel Permission Optimization
`/chanoptz` is a review-first flow. It requires a target channel parameter, loads every guild member, snapshots the channel overrides, validates a local permission simulation against JDA's explicit channel permissions/access for the current state, and only proposes removals that keep every member's access and explicit channel permission set unchanged. The heavy analysis runs on virtual threads, and the review summary reports both the total number of redundant permission entries found and the optimization percentage. The approval step is guarded by an in-memory review plan plus an override signature check so stale reviews are rejected instead of applying against a changed channel.

## Bot Oficina Dorme
The Oficina Dorme mini-game is in-memory and channel-scoped. Each match is keyed by the main event text channel, while private role coordination happens in private threads created under that same channel. The rules layer is intentionally separated from the Discord layer so round resolution, role balancing, vote ties, and victory conditions can be verified with unit tests.

Operationally, the feature has a hard cap of 25 players because Discord string select menus support at most 25 options and the match uses select menus for all automated voting. Private role threads invite the role members, the host, and all members with EVENT-scope staff roles. If any required action channel is deleted, the match is terminated immediately. If a participant leaves the guild, is kicked, is banned, or confirms the day-vote leave button, they are removed from the in-memory roster, outstanding votes targeting them are invalidated, and the match is re-evaluated for immediate victory or phase progression.

`/oficinadorme create` can receive an optional `announcement-channel` text channel. Public lifecycle announcements such as night starts, day summaries, deaths, eliminations, departure notices, and game-over messages are sent there when configured; voting UI remains in the main match channel. Detective investigation results are delayed until the public day summary and are no longer posted in the detective thread. When a custom announcement channel exists, the end of the match also posts a host-only summary with log download and private-thread deletion buttons.

Every relevant match action is persisted to `game_mafia_logs` through `GameMafiaLogRepository`, using an English `action` string plus structured fields such as event type, actor, target, channel, phase, and timestamp so moderation can inspect the full match timeline later.

## Bot Giveaways
`/giveaway` is a `Manage Server` command with `create`, `end`, and `reroll` subcommands. Creation collects uniform options in the slash command, then opens a prize-specific modal for generic, economy money, or color-role details. Giveaway participation and claim controls use durable component ids prefixed with `giveaway:` instead of the temporary composed interaction store, because active giveaways must survive beyond a single in-memory context.

Active giveaway embeds show the prize, host, end timestamp, winner count, entry count, and optional required voice channel. Entry updates are routed through `ThrottledAction`, so rapid participation changes coalesce into at most one Discord message edit per update window. If a giveaway requires a voice channel, joining is rejected unless the member is currently connected there, and voice-state updates remove existing entries as soon as the member leaves or moves away from that required channel.

When a giveaway ends, the service marks it ended, draws winners from current entries, persists winners, edits the giveaway message, and posts an announcement. Generic prizes are marked for manual fulfillment. Economy prizes stay pending until a winner clicks claim and chooses Oficina or UnbelievaBoat; the selected economy is credited directly to the winner's bank. Color-role prizes stay pending until a winner chooses a configured color role through a string select menu populated only from registered color roles; the role is applied and persisted in `color_roles_state`.

Color role expiration now uses `color_roles_state.expires_at`. New shop purchases keep the existing 60-day default and giveaway color prizes store their own configured expiration. The daily `ColorRoleRemotionHandler` removes expired roles and deletes the persisted state after successful Discord removal. If the configured role no longer exists in the guild, the handler treats that row as stale data, logs the missing role, and deletes the exact `guild_id`/`user_id`/`role_id` row to avoid accumulating ghost color-role records.

`/accumulator` is a `Ban Members` command for staging event prizes before payout. `/accumulator add` requires a target user and creates exactly one persistent `PENDING` row per invocation in `accumulator_prizes` for a money or temporary color-role prize; `/accumulator list` renders the pending box with durable `acc:v1:` controls, six rows per page, sorted by prize type priority and then money amount descending. Rows are never deleted: refused prizes become `REJECTED`, successful payouts become `PAID`, and both individual and approve-all payouts store `approved_by`/`approved_at`.

Accumulator money prizes require an integer amount from 1 to 1,000,000 and are paid to the selected provider's bank balance. Switching the selected money provider or rejecting a row edits the durable list message in place without posting a private confirmation. Color-role prizes store their duration when added, then the creator or a `Manage Server` user chooses the exact configured color role from the list controls before payout. Paying one row or approving all rows is restricted to `Manage Server`; approve-all validates every pending row before making changes, pays as a single best-effort atomic batch with rollback hooks for reversible external effects, and returns exactly one public report embed.

The `/colors` command is the color-role shop surface. It sends a public Components V2 container built by `ColorRoleStoreMessageFactory`: each row shows the configured role, the expiry timestamp when the user already owns it, and a buy/remove button. Those row buttons only open public confirmation messages; the final confirmation buttons still route through the existing `ColorRolePurchaseHandler` and `ColorRoleRemoveHandler`, so charging, refunding, persistence, and Discord role mutation remain centralized.

`/userinfo` sends a normal message-level `Liberar Contagem` button for the displayed member. The button is stored in the temporary interaction memory with the default five-minute retention and is restricted to that member. Confirming release asks the member to choose Oficina or UnbelievaBoat, charges `2,000` from the selected provider's bank through `PaymentManagerProvider`, then removes the role configured by `fun.counting.punishments.role.id`. Failed Discord role removal rolls the charge back when the selected provider exposes a rollback action.

`/userinfo` resolves `Entrou no Servidor` from `member_join_events` using the earliest known `created_at` for the target user. `MemberJoinUpsert` appends a row for every future `GuildMemberJoinEvent`, allowing multiple rows for users who leave and rejoin.

## Bot Nickname Approval
Nickname requests are split between validation and approval. Messages in `channels.nick-update.id` are checked with `emoji-java`; requests with more than three emojis or unauthorized staff-owned emojis receive a pt-BR embed reply and a rejection reaction. `/nick` defaults to `Gerenciar Apelidos`, validates the target nickname, rejects bot targets, rejects staff targets, blocks targets above the issuer's role hierarchy with `Member.canInteract`, and sends a durable approval embed to `channels.staff-nick-update.id`. Unauthorized staff emojis pause the slash command behind an ephemeral embed confirmation before the request can be queued.

Approval and rejection buttons use IDs prefixed with `nick-`, so the durable listener can ignore unrelated component clicks without a database read. Pending requests are stored with the message id, approve/reject button ids, target, submitter, requested nickname, emoji authorization summaries, status, and decision metadata. Approving changes the member nickname with audit reason `Requested by: <staff id>` and edits the approval message green; rejecting edits it red.

## Bot Shared Utilities
`UrlBuilder` is a small query-string utility for features that need to safely inspect or mutate URLs without hand-splicing strings. It preserves the original URI structure, stores decoded query parameters in insertion order, supports fluent updates through `set`, `add`, `remove`, and `clear`, and can build either a `URI` via `toUri()` or a string via `build()` and `toString()`. The utility is intentionally single-value per key; if a feature needs repeated query keys, extend it deliberately instead of quietly changing its semantics.

`ThrottledAction<T>` is a generic latest-value coalescer. Each `post(T)` replaces the pending value, and the scheduled flush runs only the latest value for that interval. It owns a scheduler and exposes `shutdown()`/`close()` so long-lived features can release it when the related workflow ends.

## Backend Boot Flow
The backend entrypoint loads configuration, creates a signal-aware root context, builds the application server through `backend/cmd/internal/app`, and starts Echo through `Server.Start`. The app package owns Playwright startup, route registration, and graceful shutdown. `Server.Close` explicitly releases the card renderer browser and Playwright runtime.

The backend container runs as non-root `appuser` with a real writable home directory. The image installs Debian Chromium and pre-bakes the Playwright Go driver into `/var/lib/oficina/backend/playwright-driver`; runtime sets `PLAYWRIGHT_DRIVER_PATH`, `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH`, `HOME`, and `XDG_CACHE_HOME` so startup does not try to write under an unmanaged `/home/appuser` path or download browser bundles. Application code verifies the baked driver files before calling `playwright.Run`, does not call `playwright.Install` at runtime, uses `SkipInstallBrowsers`, and launches the system Chromium executable.

Backend Compose mounts a host-side `static/` directory next to the compose file into `/app/static:ro`, because the Playwright templates and image assets are runtime inputs. Ansible keeps that directory populated from `backend/static/` through the runtime role's `oficina_compose_assets` list before deploying the stack.

HTTP handlers receive dependencies through small interfaces instead of calling concrete service functions directly. The level card routes use an injected card renderer, which keeps route behavior testable without launching Playwright.

## Backend API Surface
The backend intentionally exposes only:
- `GET /health` for unauthenticated liveness checks.
- `POST /api/levels/cards` for level profile card screenshots.
- `POST /api/levels/roles` for level-role list screenshots.
- `/static/*` for the HTML templates and image assets consumed by Playwright.

Shared HTTP middleware adds request IDs, recovery, JSON request logs, body limits, wildcard CORS without browser credentials, and a basic in-memory rate limiter. There is no CSRF middleware because the backend no longer has cookie-backed mutating admin routes.

Unauthenticated `GET /health` returns a small `{"status":"ok"}` response for backend liveness checks. The backend Docker image and compose definition both probe this route with `curl`.

## Backend Persistence
The backend application currently has no database dependency and does not open MySQL at runtime. Do not add backend tables to the bot migration stream unless backend persistence is deliberately reintroduced.

The backend does not currently include admin users, Discord OAuth/REST metadata, service-token sync APIs, dashboard APIs, config synchronization, repositories, or video downloads. Reintroducing any of those surfaces should start with an explicit design and tests instead of reviving the removed legacy code paths by habit.

## History Preservation
This mono-repo was assembled with history-preserving subtree imports:
- `OficinaMyuu/OficinaImagery` was imported under `backend/`.
- `OficinaMyuu/RegistroOficina` was imported under `registrar/`.

Use non-squashed subtree-style merges for future imports that must preserve source repository history.
