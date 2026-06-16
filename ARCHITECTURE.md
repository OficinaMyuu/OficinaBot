# ARCHITECTURE.md

## Overview
OficinaServices is the mono-repo for Oficina's Discord-facing services and shared backend work. Keep this file as the single architecture guide for the repository; do not add child `ARCHITECTURE.md` files.

## Services
- `bot/`: Java 21 Discord bot using JDA 6, Maven, SQLite, jOOQ, HikariCP, Quartz, OkHttp, and the OpenAI Java SDK.
- `backend/`: Go HTTP backend that started as OficinaImagery and is expected to take on broader API responsibilities. Its current entrypoint is `backend/cmd/api/main.go`, with server bootstrapping under `backend/cmd/internal/app/`.
- `registrar/`: Java 17 Discord registration service using JDA 5 and Maven. Its entrypoint is `registrar/src/main/java/ofc/bot/RegisterMaster.java`.

## Repository Structure
- Repo-level GitHub Actions workflows live in `.github/workflows/`.
- Service source, build descriptors, and service-owned assets live inside each service directory.
- Runtime files, generated artifacts, local databases, and package outputs are ignored and are not source of truth.

## Deployment Model
- The bot workflow builds `bot/target/bot.jar`, uploads it through the Oficina SFTP secrets, and restarts `PTERO_OFICINA_SERVER_ID`.
- The registrar workflow builds `registrar/target/bot.jar`, uploads it through the Registry SFTP secrets, and restarts `PTERO_REGISTRY_SERVER_ID`.
- Backend deployment is intentionally left undefined while the backend responsibilities are expanded.

## Bot Boot Flow
1. `bot/src/main/java/ofc/bot/Main.java` loads local files through `BotFiles.loadFiles()`.
2. `DB.init()` creates/connects the SQLite datasource and creates all known tables.
3. JDA is built and awaited.
4. Console handler and Quartz jobs are initialized.
5. Services, listeners, slash commands, and composed interactions are registered.
6. OpenAI client is created from `openai.key`.

## Bot Architectural Pieces
- Entry point: `bot/src/main/java/ofc/bot/Main.java`
- Application wiring: `bot/src/main/java/ofc/bot/handlers/EntityInitializerManager.java`
- Slash command registration: `bot/src/main/java/ofc/bot/handlers/interactions/commands/slash/CommandsInitializer.java`
- Persistence bootstrap: `bot/src/main/java/ofc/bot/domain/sqlite/DB.java`
- Repository access: `bot/src/main/java/ofc/bot/domain/sqlite/repository/Repositories.java`
- Shared utility helpers: `bot/src/main/java/ofc/bot/util/`

## Bot Persistence Shape
- SQLite database file: `database.db`
- Table definitions live under `bot/src/main/java/ofc/bot/domain/tables/`
- Entity models live under `bot/src/main/java/ofc/bot/domain/entity/`
- Repository implementations live under `bot/src/main/java/ofc/bot/domain/sqlite/repository/`
- Runtime configuration is partially stored in the SQLite `config` table and accessed via `BotProperties`.
- Schema migrations are performed manually outside `DB.java`; startup only creates missing tables.

## Bot Interaction Model
- Slash commands live under `bot/src/main/java/ofc/bot/commands/impl/slash/`.
- JDA event listeners live under `bot/src/main/java/ofc/bot/listeners/`.
- Button/modal/menu/autocomplete routing is centralized under `bot/src/main/java/ofc/bot/handlers/interactions/`.
- Scheduled jobs live under `bot/src/main/java/ofc/bot/jobs/`.

Voice message transcriptions are offered by `MessageTranscriptionsHandler` through the microphone reaction. The listener skips automatic reactions for voice-message authors listed in `messages.transcriptions.banned-user-ids`, and it also rejects manual microphone reactions on those authors' messages before cached or newly generated transcript output is sent. The key is read with `Bot.getArray(...)` and stores raw Discord user IDs.

Coinflip message inference is handled by `CoinflipInferenceHandler` for plain `cara`/`coroa` messages. Channels listed in `messages.coinflip.banned-channel-ids` are ignored before pending flips or cooldowns are touched. The key is read with `Bot.getArray(...)` and stores raw Discord channel IDs.

Attachment preservation is handled by `AttachmentForwardingLogger`. When a non-bot, non-webhook guild message contains attachments, the bot forwards that message with JDA's native `Message#forwardTo(...)` API to the text channel configured by `channels.attachments-log.id`. The feature is intentionally stateless: it does not create a database table, scan the archive channel, or download/re-upload attachment bytes.

## Bot Tickets
Ticket creation sends a durable initial message with add-member, remove-member, and close controls. Close opens the existing close-reason modal. Add/remove member controls are handled by durable component IDs in `TicketMemberManagementHandler` instead of the temporary interaction memory manager, because ticket messages can outlive a bot process restart. Adding skips users that already have ticket access. Removing deletes only explicit member permission overrides, skips the ticket initiator, and ignores administrators.

## Bot Economy
Automated income is split by economy provider: `ChatMoneyHandler` credits Oficina wallet money for eligible guild messages, while `VoiceChatMoneyHandler` credits UnbelievaBoat cash or bank money for eligible voice activity. Voice channels configured in `income.voice.bank-channel-ids` are semicolon-separated Discord channel IDs that pay the doubled voice income amount to bank instead of cash. Both paths honor `PolicyType.BLOCK_MONEY_GAINS` through `AutomatedMoneyGainPolicy`, matching blocked users, roles, or channels. The policy is intentionally limited to automated income and does not block explicit command rewards such as `/daily` and `/work`, nor manual or claim-based prize fulfillment such as giveaway money claims.

UnbelievaBoat access is intentionally isolated behind `UnbelievaBoatClient` and `UnbelievaBoatRequester`. The client owns guild-scoped economy operations and sends the configured raw API token in the `Authorization` header, matching UnbelievaBoat's documented auth format. The requester owns HTTP retry behavior: successful responses may pause when `X-RateLimit-Remaining` is nearly exhausted, while HTTP 429 responses retry with the JSON `retry_after` delay first, then `X-RateLimit-Reset`, then local exponential backoff.

`/rob` only steals from the target user's wallet. The failure probability follows the UnbelievaBoat-style formula `robber net worth / (target wallet + robber net worth)`, clamped to `20%` through `80%`. On success, the stolen amount is the success probability multiplied by the target wallet and rounded up. On failure, the robber is fined using the UnbelievaBoat crime default range of `20%` through `40%` of their net worth. The fine is applied to bank balance, because wallet cannot be negative while bank balance is allowed to represent debt.

`/bets roulette` is a timed, channel-scoped betting lobby modeled after UnbelievaBoat roulette. The first accepted bet opens a 30-second shared spin; other users, and the same user, can place more bets in that lobby before the ball lands. Supported spaces are red/black, even/odd, 1-18/19-36, 1st/2nd/3rd columns, 1-12/13-24/25-36 dozens, and exact numbers 0 through 36. Stakes are deducted from bank immediately and accepted amounts range from 100 to 1,000,000. Stake input uses the shared economy amount parser, so shorthand such as `all`, `2k`, and `750k` resolves against the bettor's bank before the roulette bounds are applied. Winners receive `stake * multiplier` back to bank on settlement, using x2, x3, or x36 according to the selected space. Settlement leaves the final lobby message visible and sends a separate result embed with winner and loser fields, omitting either field when that group is empty. Completed spins reuse the existing `bet_games` and `games_participants` tables with `GameType.ROULETTE`.

## Bot Moderation
Automod warnings are persisted before the current threshold is resolved through `AutomodActionRepository`. When the configured threshold resolves to `KICK`, the Discord kick is queued first, and cleanup runs only after JDA reports that kick as successful. Cleanup deletes the user's XP row from `users_xp` and resets every configured economy account to zero, currently Oficina Bank and UnbelievaBoat.

## Bot Levels
`LevelManager` grants XP, persists level progress, announces level-ups in the configured level-up channel, and applies matching level roles. Users can run `.toggle-rankup-pings` to control whether their level-up announcement mentions them. The command is a guild legacy listener available to every user, stores the deterministic boolean value in `users_preferences.rankup_pings_enabled`, and preserves `users_preferences.locale`, which can remain null until Discord exposes it through an interaction.

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

## Bot Nickname Approval
Nickname requests are split between validation and approval. Messages in `channels.nick-update.id` are checked with `emoji-java`; requests with more than three emojis or unauthorized staff-owned emojis receive a pt-BR embed reply and a rejection reaction. `/nick` defaults to `Gerenciar Apelidos`, validates the target nickname, rejects bot targets, rejects staff targets, blocks targets above the issuer's role hierarchy with `Member.canInteract`, and sends a durable approval embed to `channels.staff-nick-update.id`. Unauthorized staff emojis pause the slash command behind an ephemeral embed confirmation before the request can be queued.

Approval and rejection buttons use IDs prefixed with `nick-`, so the durable listener can ignore unrelated component clicks without a database read. Pending requests are stored with the message id, approve/reject button ids, target, submitter, requested nickname, emoji authorization summaries, status, and decision metadata. Approving changes the member nickname with audit reason `Requested by: <staff id>` and edits the approval message green; rejecting edits it red.

## Bot Shared Utilities
`UrlBuilder` is a small query-string utility for features that need to safely inspect or mutate URLs without hand-splicing strings. It preserves the original URI structure, stores decoded query parameters in insertion order, supports fluent updates through `set`, `add`, `remove`, and `clear`, and can build either a `URI` via `toUri()` or a string via `build()` and `toString()`. The utility is intentionally single-value per key; if a feature needs repeated query keys, extend it deliberately instead of quietly changing its semantics.

`ThrottledAction<T>` is a generic latest-value coalescer. Each `post(T)` replaces the pending value, and the scheduled flush runs only the latest value for that interval. It owns a scheduler and exposes `shutdown()`/`close()` so long-lived features can release it when the related workflow ends.

## Backend Boot Flow
The backend entrypoint loads configuration, creates a signal-aware root context, builds the application server through `backend/cmd/internal/app`, and starts Echo through `Server.Start`. The app package owns SQLite startup/migrations, Playwright installation/startup, route registration, and graceful shutdown. `Server.Close` explicitly releases the database handle, card renderer browser, and Playwright runtime.

HTTP handlers receive dependencies through small interfaces instead of calling concrete service functions directly. The level card routes use an injected card renderer, and the external video route uses an injected downloader. This keeps route behavior testable without launching Playwright or shelling out to `yt-dlp`.

## Backend Persistence
The backend uses SQLite with WAL mode and embedded goose migrations. The default database path is `./data/oficina-services.db` relative to the backend process, and `DATABASE_PATH` can override it for deployment. Startup opens the database, enables foreign keys, WAL, busy timeout, and normal synchronous mode, then applies migrations before HTTP routes start.

The repository layer uses GORM models mapped to migration-owned tables. Current persistence tables cover admin users, bot clients, event batches, message logs, punishments, config versions, config acknowledgements, and audit actions. Tests for persistence use real temporary SQLite databases with migrations applied, not mocks.

## Backend Admin Auth
Admin login uses Discord OAuth2 with the `identify` scope. The backend stores allowlisted admins in `users`; the configured `OFICINA_OWNER_DISCORD_ID` can bootstrap itself on first login and is the only account allowed to add or remove other admins. There is intentionally no role or permission system in this phase.

Admin sessions are stored in `admin_sessions` with hashed session tokens, expiration timestamps, and last-seen timestamps. The browser receives an HttpOnly SameSite=Lax session cookie. `SESSION_COOKIE_SECURE` defaults to true and should only be disabled for local HTTP development.

Auth routes live under `/api/auth/*`: Discord login start/callback, current admin lookup, and logout. Owner-only admin management lives under `/api/admin/users`.

## Backend Service Auth
Bot-to-backend APIs live under `/api/service/*` and use `Authorization: Bearer <token>`. The backend hashes bearer tokens with SHA-256 and compares them against `bot_clients.token_hash`; raw service tokens are not stored. Successful service authentication updates the client `last_seen_at` timestamp and stores the authenticated client in the Echo context for service handlers.

The protected service endpoints include `/api/service/me`, batch ingestion for message logs, punishments, registrations, sync heartbeats, pending config polling, and config ACKs. Batch ingestion requires caller-provided `batch_id` values. A repeated batch id returns success without inserting duplicate rows, so bots can retry network failures without inventing ghosts.

Shared HTTP middleware adds request IDs, recovery, JSON request logs, body limits, CORS for the configured frontend origin, CSRF checks for cookie-backed mutating admin routes, and a basic in-memory rate limiter. CSRF is intentionally not applied to bearer-token service routes.

## Backend Dashboard APIs
Admin dashboard APIs live under `/api/dashboard/*` and require the admin session cookie. Current read endpoints expose recent message logs, punishments, registrations, sync health, audit actions, and config versions with simple limit-based pagination.

Config writes create immutable `config_versions` rows and audit actions. Bots poll `/api/service/configs/pending` for unapplied config versions and ACK each applied version through `/api/service/configs/:version_id/ack`. Polling is non-destructive; data is not removed just because a bot asked for it.

## Backend Discord REST Metadata
The backend has a REST-only Discord integration built with `discordgo` and authenticated by `DISCORD_BOT_TOKEN`. It creates a `discordgo.Session` for HTTP methods only and must not call `Session.Open()`, configure gateway intents, or connect to Discord websockets. The Discord-facing bots remain responsible for gateway events.

Dashboard metadata endpoints expose trimmed guild, channel, role, and user DTOs under `/api/dashboard/discord/*`. Metadata is cached in-process for five minutes to reduce repeated Discord REST calls; the cache is intentionally local and disposable.

## History Preservation
This mono-repo was assembled with history-preserving subtree imports:
- `OficinaMyuu/OficinaImagery` was imported under `backend/`.
- `OficinaMyuu/RegistroOficina` was imported under `registrar/`.

Use non-squashed subtree-style merges for future imports that must preserve source repository history.
