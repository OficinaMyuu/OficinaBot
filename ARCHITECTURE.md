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

## Events Command
- Slash entrypoint:
  `src/main/java/ofc/bot/commands/impl/slash/ToggleEventsCommand.java`
- Staff role helpers:
  `src/main/java/ofc/bot/util/content/Staff.java`
- Config keys:
  `channels.events.text.id` and `channels.events.voice.id`

`/events` toggles send/connect permissions for the configured event text and voice channels. The optional `disconnect` argument is only applied while closing an event and defaults to `No one`, preserving member voice state unless an operator chooses a cleanup policy. `Keep Staff` preserves members with the global staff role, while `Keep Event Staff` preserves members with any role from `Staff.Scope.EVENTS`.

## Moderation
- Automod listener:
  `src/main/java/ofc/bot/listeners/discord/moderation/AutoModerator.java`
- Punishment orchestration:
  `src/main/java/ofc/bot/handlers/moderation/PunishmentManager.java`
- Auto-kick cleanup:
  `src/main/java/ofc/bot/handlers/moderation/AutoKickCleanup.java`

Automod warnings are persisted before the current threshold is resolved through `AutomodActionRepository`. When the configured threshold resolves to `KICK`, the Discord kick is queued first, and the cleanup runs only after JDA reports that kick as successful. Cleanup deletes the user's XP row from `users_xp` and resets every configured economy account to zero, currently Oficina Bank and UnbelievaBoat.

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

Operationally, the feature has a hard cap of 25 players because Discord string select menus support at most 25 options and the match uses select menus for all automated voting. Private role threads invite the role members, the host, and all members with EVENT-scope staff roles. If any required action channel is deleted, the match is terminated immediately. If a participant leaves the guild, is kicked, is banned, or confirms the day-vote leave button, they are removed from the in-memory roster, outstanding votes targeting them are invalidated, and the match is re-evaluated for immediate victory or phase progression.

`/oficinadorme create` can receive an optional `announcement-channel` text channel. Public lifecycle announcements such as night starts, day summaries, deaths, eliminations, departure notices, and game-over messages are sent there when configured; voting UI remains in the main match channel. Detective investigation results are delayed until the public day summary and are no longer posted in the detective thread. When a custom announcement channel exists, the end of the match also posts a host-only summary with log download and private-thread deletion buttons.

Every relevant match action is also persisted to `game_mafia_logs` through `GameMafiaLogRepository`, using an English `action` string plus structured fields such as event type, actor, target, channel, phase, and timestamp so moderation can inspect the full match timeline later.

## Coinflip Inference
- Listener:
  `src/main/java/ofc/bot/listeners/discord/guilds/messages/CoinflipInferenceHandler.java`
- Unit tests:
  `src/test/java/ofc/bot/listeners/discord/guilds/messages/CoinflipInferenceHandlerTest.java`

The coinflip inference listener watches guild messages for plain `cara` and `coroa` guesses, pairs two different users with opposite guesses inside a short timeout window, and announces the result in-channel. The cooldown remains channel-scoped for regular users, but a matching pair bypasses that cooldown when either participant is staff, which keeps moderation and event facilitation from getting rate-limited by the mini-interaction.

## Message Transcriptions
- Listener:
  `src/main/java/ofc/bot/listeners/discord/guilds/messages/MessageTranscriptionsHandler.java`
- Unit tests:
  `src/test/java/ofc/bot/listeners/discord/guilds/messages/MessageTranscriptionsHandlerTest.java`

Voice-message transcription is requested through the microphone reaction. The listener validates that the source message is still a voice message with a supported audio attachment before downloading or calling OpenAI. Transcription work is guarded by an in-memory single-flight set keyed by Discord message id, so concurrent reactions for the same message coalesce into one download/OpenAI transcription workflow. Completed transcriptions are persisted in `message_transcriptions`, and resend attempts use the stored transcription with a short anti-flood cooldown.

## Nickname Approval
- Slash entrypoint:
  `src/main/java/ofc/bot/commands/impl/slash/NickCommand.java`
- Validation and queue dispatch:
  `src/main/java/ofc/bot/handlers/nick/NicknameEmojiPolicy.java`
  and `src/main/java/ofc/bot/handlers/nick/NicknameRequestDispatcher.java`
- User request channel guard:
  `src/main/java/ofc/bot/listeners/discord/guilds/messages/NicknameUpdateRequestGuard.java`
- Durable approval buttons:
  `src/main/java/ofc/bot/listeners/discord/interactions/buttons/nick/NicknameApprovalButtonListener.java`
- Confirmation button:
  `src/main/java/ofc/bot/listeners/discord/interactions/buttons/nick/NicknameSendAnywayHandler.java`
- Persistence:
  `nickname_update_requests`

Nickname requests are split between validation and approval. Messages in `channels.nick-update.id` are checked with `emoji-java`; requests with more than three emojis or unauthorized staff-owned emojis receive a pt-BR embed reply and a rejection reaction. `/nick` defaults to `Gerenciar Apelidos`, validates the target nickname, rejects bot targets, rejects staff targets, blocks targets above the issuer's role hierarchy with `Member.canInteract`, and sends a durable approval embed to `channels.staff-nick-update.id`. Unauthorized staff emojis pause the slash command behind an ephemeral embed confirmation before the request can be queued.

Approval and rejection buttons use IDs prefixed with `nick-`, so the durable listener can ignore unrelated component clicks without a database read. Pending requests are stored with the message id, approve/reject button ids, target, submitter, requested nickname, emoji authorization summaries, status, and decision metadata. Approving changes the member nickname with audit reason `Requested by: <staff id>` and edits the approval message green; rejecting edits it red.

## Operational Notes
- Build output is a shaded jar at `target/bot.jar`
- CI deploy workflow is defined in `.github/workflows/deploy.yml`
- Local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, and `target/` should not be read for code understanding; they are runtime/generated artifacts, not source of truth

## Shared Utilities
- `src/main/java/ofc/bot/util/UrlBuilder.java`

`UrlBuilder` is a small query-string utility for features that need to safely inspect or mutate URLs without hand-splicing strings. It preserves the original URI structure, stores decoded query parameters in insertion order, supports fluent updates through `set`, `add`, `remove`, and `clear`, and can build either a `URI` via `toUri()` or a string via `build()` and `toString()`. The utility is intentionally single-value per key; if a feature needs repeated query keys, extend it deliberately instead of quietly changing its semantics.

## Giveaways
- Slash entrypoint:
  `src/main/java/ofc/bot/commands/impl/slash/giveaway/`
- Runtime services and UI factories:
  `src/main/java/ofc/bot/handlers/giveaway/`
- Durable interaction listener:
  `src/main/java/ofc/bot/listeners/discord/interactions/buttons/giveaway/GiveawayInteractionListener.java`
- Voice condition cleanup:
  `src/main/java/ofc/bot/listeners/discord/guilds/voice/GiveawayVoiceConditionListener.java`
- Scheduled ending:
  `src/main/java/ofc/bot/jobs/GiveawayEndHandler.java`
- Persistence:
  `giveaways`, `giveaway_entries`, and `giveaway_winners`

`/giveaway` is a `Manage Server` command with `create`, `end`, and `reroll` subcommands. Creation collects uniform options in the slash command, then opens a prize-specific modal for generic, economy money, or color-role details. Giveaway participation and claim controls use durable component ids prefixed with `giveaway:` instead of the temporary composed interaction store, because active giveaways must survive beyond a single in-memory context.

Active giveaway embeds show the prize, host, end timestamp, winner count, entry count, and optional required voice channel. Entry updates are routed through `ThrottledAction`, so rapid participation changes coalesce into at most one Discord message edit per update window. If a giveaway requires a voice channel, joining is rejected unless the member is currently connected there, and voice-state updates remove existing entries as soon as the member leaves or moves away from that required channel.

When a giveaway ends, the service marks it ended, draws winners from current entries, persists winners, edits the giveaway message, and posts an announcement. Generic prizes are marked for manual fulfillment. Economy prizes stay pending until a winner clicks claim and chooses Oficina or UnbelievaBoat; the selected economy is credited directly to the winner's bank. Color-role prizes stay pending until a winner chooses a configured color role through a string select menu populated only from registered color roles; the role is applied and persisted in `color_roles_state`.

Color role expiration now uses `color_roles_state.expires_at`. Existing rows are migrated at startup by backfilling `updated_at + 60 days`, while new shop purchases keep the existing 60-day default and giveaway color prizes store their own configured expiration. The daily `ColorRoleRemotionHandler` removes expired roles and deletes the persisted state after successful Discord removal. If the configured role no longer exists in the guild, the handler treats that row as stale data, logs the missing role, and deletes the exact `guild_id`/`user_id`/`role_id` row to avoid accumulating ghost color-role records.

## Shared Throttling
- Utility:
  `src/main/java/ofc/bot/handlers/ThrottledAction.java`
- Unit tests:
  `src/test/java/ofc/bot/handlers/ThrottledActionTest.java`

`ThrottledAction<T>` is a generic latest-value coalescer. Each `post(T)` replaces the pending value, and the scheduled flush runs only the latest value for that interval. It owns a scheduler and exposes `shutdown()`/`close()` so long-lived features can release it when the related workflow ends.
