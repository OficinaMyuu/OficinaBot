package ofc.bot.handlers.games.mafia.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ThreadChannelAction;
import ofc.bot.handlers.games.mafia.discord.MafiaComponentFactory;
import ofc.bot.handlers.games.mafia.discord.MafiaMessageFactory;
import ofc.bot.handlers.games.mafia.domain.DayResolution;
import ofc.bot.handlers.games.mafia.domain.MafiaMatch;
import ofc.bot.handlers.games.mafia.domain.MafiaPlayer;
import ofc.bot.handlers.games.mafia.domain.MafiaRoleConfiguration;
import ofc.bot.handlers.games.mafia.domain.NightResolution;
import ofc.bot.handlers.games.mafia.enums.MafiaEventType;
import ofc.bot.handlers.games.mafia.enums.MafiaPhase;
import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import ofc.bot.handlers.games.mafia.enums.MafiaTeam;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central in-memory coordinator for Oficina Dorme matches.
 * <p>
 * This class owns the active match registry, bridges Discord events to the pure rules engine, and is responsible
 * for announcing lifecycle changes such as match start, win conditions, channel deletion, and member departures.
 */
public final class MafiaGameManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MafiaGameManager.class);
    private static final MafiaGameManager INSTANCE = new MafiaGameManager();

    private final MafiaGameLogger gameLogger = MafiaGameLogger.getInstance();
    private final MafiaMatchEngine matchEngine = new MafiaMatchEngine();
    private final Map<Long, MafiaMatch> matchesByMainChannel = new ConcurrentHashMap<>();
    private final Map<Long, Long> mainChannelByThreadId = new ConcurrentHashMap<>();

    /**
     * Creates the singleton manager.
     */
    private MafiaGameManager() {}

    /**
     * Returns the singleton manager instance.
     *
     * @return global match manager
     */
    public static MafiaGameManager getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the pure rules engine used by this manager.
     *
     * @return rules engine
     */
    public MafiaMatchEngine getMatchEngine() {
        return matchEngine;
    }

    /**
     * Indicates whether the provided main channel already has an active match.
     *
     * @param mainChannelId main event text channel id
     * @return {@code true} when a match is already registered for that channel
     */
    public boolean hasActiveMatch(long mainChannelId) {
        return matchesByMainChannel.containsKey(mainChannelId);
    }

    /**
     * Creates and registers a new lobby-scoped match.
     *
     * @param guildId guild id that owns the match
     * @param mainChannelId main event text channel id
     * @param hostId lobby creator user id
     * @param maxPlayers configured player cap
     * @param requestedRoleConfiguration optional manual role configuration
     * @return newly created match
     */
    public MafiaMatch createMatch(long guildId, long mainChannelId, long hostId, int maxPlayers,
                                  @Nullable MafiaRoleConfiguration requestedRoleConfiguration) {
        MafiaMatch match = new MafiaMatch(guildId, mainChannelId, hostId, maxPlayers, requestedRoleConfiguration);
        matchesByMainChannel.put(mainChannelId, match);
        return match;
    }

    /**
     * Returns the match registered directly on the provided main channel.
     *
     * @param mainChannelId main event text channel id
     * @return registered match, or {@code null} when none exists
     */
    @Nullable
    public MafiaMatch getMatchByMainChannel(long mainChannelId) {
        return matchesByMainChannel.get(mainChannelId);
    }

    /**
     * Resolves a match by either its main channel or one of its private role threads.
     *
     * @param channelId channel id to resolve
     * @return matching match, or {@code null} when the channel is unrelated to any match
     */
    @Nullable
    public MafiaMatch getMatch(long channelId) {
        MafiaMatch directMatch = matchesByMainChannel.get(channelId);
        if (directMatch != null) {
            return directMatch;
        }

        Long mainChannelId = mainChannelByThreadId.get(channelId);
        return mainChannelId == null ? null : matchesByMainChannel.get(mainChannelId);
    }

    /**
     * Removes the match from the active registry and clears any thread-to-main-channel mappings.
     *
     * @param mainChannelId main event text channel id used as the registry key
     */
    public void endMatch(long mainChannelId) {
        MafiaMatch match = matchesByMainChannel.remove(mainChannelId);
        if (match == null) {
            return;
        }

        for (long threadId : match.getPrivateThreadIds()) {
            mainChannelByThreadId.remove(threadId);
        }
    }

    /**
     * Starts a lobby by resolving roles, creating private threads, adding role members, and announcing the first night.
     *
     * @param match match being started
     * @param guild guild that owns the match
     * @param hook interaction hook used to report the startup result
     */
    public void startMatch(MafiaMatch match, Guild guild, InteractionHook hook) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null) {
            hook.editOriginal("Não foi possível encontrar o canal principal da partida.").queue();
            endMatch(match.getMainChannelId());
            return;
        }

        MafiaRoleConfiguration resolvedConfiguration;
        try {
            resolvedConfiguration = matchEngine.resolveConfiguration(match.getRequestedRoleConfiguration(), match.getPlayerCount());
        } catch (IllegalArgumentException exception) {
            hook.editOriginal(exception.getMessage()).queue();
            return;
        }

        synchronized (match) {
            match.setActiveRoleConfiguration(resolvedConfiguration);
            assignRoles(match, resolvedConfiguration);
            match.startNightPhase();
        }
        gameLogger.log(match, MafiaEventType.NIGHT_STARTED, "Night phase started.", null, null, match.getMainChannelId());

        RestAction.allOf(
                createPrivateThread(mainChannel, MafiaRole.ASSASSIN),
                createPrivateThread(mainChannel, MafiaRole.DOCTOR),
                createPrivateThread(mainChannel, MafiaRole.DETECTIVE)
        ).queue(result -> {
            ThreadChannel assassinThread = result.get(0);
            ThreadChannel doctorThread = result.get(1);
            ThreadChannel detectiveThread = result.get(2);

            synchronized (match) {
                match.setPrivateThreadId(MafiaRole.ASSASSIN, assassinThread.getIdLong());
                match.setPrivateThreadId(MafiaRole.DOCTOR, doctorThread.getIdLong());
                match.setPrivateThreadId(MafiaRole.DETECTIVE, detectiveThread.getIdLong());
            }

            mainChannelByThreadId.put(assassinThread.getIdLong(), match.getMainChannelId());
            mainChannelByThreadId.put(doctorThread.getIdLong(), match.getMainChannelId());
            mainChannelByThreadId.put(detectiveThread.getIdLong(), match.getMainChannelId());

            List<RestAction<?>> memberActions = buildThreadMemberActions(match, assassinThread, doctorThread, detectiveThread);
            RestAction.allOf(memberActions).queue(v -> {
                announceNightPhase(match, guild);
                hook.editOriginal("Partida iniciada. As funções e as threads privadas já estão prontas.").queue();
            }, error -> {
                LOGGER.error("Failed to add players to Oficina Dorme threads", error);
                hook.editOriginal("Não foi possível adicionar os jogadores às threads privadas.").queue();
                endMatch(match.getMainChannelId());
            });
        }, error -> {
            LOGGER.error("Failed to create Oficina Dorme private threads", error);
            hook.editOriginal("Não foi possível criar as threads privadas da partida.").queue();
            endMatch(match.getMainChannelId());
        });
    }

    /**
     * Announces the beginning of a night round in the main channel and private role threads.
     *
     * @param match active match
     * @param guild guild that owns the match
     */
    public void announceNightPhase(MafiaMatch match, Guild guild) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null) {
            return;
        }

        mainChannel.sendMessageEmbeds(MafiaMessageFactory.createNightAnnouncement(match))
                .setComponents(ActionRow.of(MafiaComponentFactory.createViewRoleButton()))
                .queue();

        sendNightMenus(match, guild, false);
    }

    /**
     * Posts refreshed night action menus when the alive roster changes during the night.
     *
     * @param match active match
     * @param guild guild that owns the match
     */
    public void announceNightMenuRefresh(MafiaMatch match, Guild guild) {
        sendNightMenus(match, guild, true);
    }

    /**
     * Announces detective-only investigation results.
     *
     * @param match active match
     * @param guild guild that owns the match
     * @param resolution resolved night result
     */
    public void announceDetectiveResults(MafiaMatch match, Guild guild, NightResolution resolution) {
        ThreadChannel detectivesThread = getPrivateThread(guild, match, MafiaRole.DETECTIVE);
        if (detectivesThread == null || resolution.investigationsByDetective().isEmpty()) {
            return;
        }

        detectivesThread.sendMessageEmbeds(MafiaMessageFactory.createDetectiveResults(match, resolution)).queue();
    }

    /**
     * Announces the start of daytime discussion after a resolved night.
     *
     * @param match active match
     * @param guild guild that owns the match
     * @param resolution resolved night result
     */
    public void announceDayDiscussion(MafiaMatch match, Guild guild, NightResolution resolution) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null) {
            return;
        }

        gameLogger.log(match, MafiaEventType.NIGHT_RESOLVED, "Night resolved.", null, null, match.getMainChannelId());
        for (Long killedPlayerId : resolution.killedPlayerIds()) {
            gameLogger.log(match, MafiaEventType.PLAYER_KILLED, "Player was killed during the night.",
                    null, killedPlayerId, match.getMainChannelId());
        }
        resolution.investigationsByDetective().values().forEach(result ->
                gameLogger.log(match, MafiaEventType.INVESTIGATION_RESOLVED,
                        result.blocked() ? "Investigation was blocked." : "Investigation resolved successfully.",
                        result.detectiveId(), result.targetId(), match.getPrivateThreadId(MafiaRole.DETECTIVE))
        );
        gameLogger.log(match, MafiaEventType.DAY_DISCUSSION_STARTED, "Day discussion started.",
                null, null, match.getMainChannelId());

        mainChannel.sendMessageEmbeds(MafiaMessageFactory.createDayDiscussion(match, resolution.killedPlayerIds()))
                .setComponents(ActionRow.of(
                        MafiaComponentFactory.createOpenDayVoteButton(),
                        MafiaComponentFactory.createViewRoleButton()
                ))
                .queue();
    }

    /**
     * Announces that day voting is open and posts the current vote menu.
     *
     * @param match active match
     * @param guild guild that owns the match
     */
    public void announceDayVote(MafiaMatch match, Guild guild) {
        sendDayVoteMessage(match, guild, false);
    }

    /**
     * Posts a refreshed day vote menu when the alive roster changes while voting is open.
     *
     * @param match active match
     * @param guild guild that owns the match
     */
    public void announceDayVoteRefresh(MafiaMatch match, Guild guild) {
        sendDayVoteMessage(match, guild, true);
    }

    /**
     * Announces the outcome of a resolved day vote.
     *
     * @param match active match
     * @param guild guild that owns the match
     * @param resolution resolved day result
     */
    public void announceDayResolution(MafiaMatch match, Guild guild, DayResolution resolution) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null) {
            return;
        }

        mainChannel.sendMessageEmbeds(MafiaMessageFactory.createDayResolution(match, resolution)).queue();
    }

    /**
     * Announces the winner in the main channel.
     *
     * @param match completed match
     * @param guild guild that owns the match
     * @param winner winning team
     */
    public void announceGameOver(MafiaMatch match, Guild guild, MafiaTeam winner) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null) {
            return;
        }

        gameLogger.log(match, MafiaEventType.GAME_WON, "Game finished with winner " + winner.name() + ".",
                null, null, match.getMainChannelId());

        mainChannel.sendMessageEmbeds(MafiaMessageFactory.createGameOver(match, winner)).queue();
    }

    /**
     * Handles the deletion of a match channel or private thread.
     * <p>
     * If a private role thread is deleted, the match is terminated in the main channel. If the main channel itself
     * is deleted, the match is terminated and the host is notified through DM when possible.
     *
     * @param api JDA instance used for fallback DM notification
     * @param guild guild where the deletion happened
     * @param deletedChannelId deleted channel id
     */
    public void handleChannelDeleted(JDA api, Guild guild, long deletedChannelId) {
        MafiaMatch match = getMatch(deletedChannelId);
        if (match == null) {
            return;
        }

        boolean mainChannelDeleted = match.getMainChannelId() == deletedChannelId;
        if (mainChannelDeleted) {
            gameLogger.log(match, MafiaEventType.CHANNEL_DELETED, "Main match channel was deleted.",
                    null, null, deletedChannelId);
            gameLogger.log(match, MafiaEventType.GAME_TERMINATED, "Game terminated because the main channel was deleted.",
                    null, null, deletedChannelId);
            endMatch(match.getMainChannelId());
            notifyHostAboutMainChannelDeletion(api, match, deletedChannelId);
            return;
        }

        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel != null) {
            gameLogger.log(match, MafiaEventType.CHANNEL_DELETED, "Required action channel was deleted.",
                    null, null, deletedChannelId);
            gameLogger.log(match, MafiaEventType.GAME_TERMINATED, "Game terminated because a required action channel was deleted.",
                    null, null, deletedChannelId);
            mainChannel.sendMessageEmbeds(
                    MafiaMessageFactory.createChannelDeletedTermination(deletedChannelId, false)
            ).queue();
        }

        endMatch(match.getMainChannelId());
    }

    /**
     * Removes a player from every active match in the guild after they leave, are kicked, or are banned.
     *
     * @param guild guild where the departure happened
     * @param userId user id that became unavailable
     * @param reason localized description of why the player left the match
     */
    public void handlePlayerUnavailable(Guild guild, long userId, String reason) {
        List<MafiaMatch> affectedMatches = matchesByMainChannel.values().stream()
                .filter(match -> match.getGuildId() == guild.getIdLong())
                .filter(match -> match.hasPlayer(userId))
                .toList();

        for (MafiaMatch match : affectedMatches) {
            handlePlayerUnavailable(match, guild, userId, reason);
        }
    }

    /**
     * Returns the private thread instance for the given role when it still exists.
     *
     * @param guild guild that owns the match
     * @param match active match
     * @param role role whose thread should be resolved
     * @return thread instance, or {@code null} when no thread is available
     */
    @Nullable
    public ThreadChannel getPrivateThread(Guild guild, MafiaMatch match, MafiaRole role) {
        Long threadId = match.getPrivateThreadId(role);
        if (threadId == null) {
            return null;
        }

        return guild.getThreadChannels().stream()
                .filter(thread -> thread.getIdLong() == threadId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Refreshes the original lobby message when the lobby roster changes outside button interactions.
     *
     * @param guild guild that owns the match
     * @param match lobby match to refresh
     */
    public void refreshLobbyMessage(Guild guild, MafiaMatch match) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null || match.getLobbyMessageId() == 0L) {
            return;
        }

        mainChannel.retrieveMessageById(match.getLobbyMessageId()).queue(
                message -> message.editMessageEmbeds(MafiaMessageFactory.createLobbyEmbed(match)).queue(),
                error -> LOGGER.debug("Could not refresh Oficina Dorme lobby message {}", match.getLobbyMessageId(), error)
        );
    }

    /**
     * Assigns randomized roles based on the resolved configuration.
     *
     * @param match match receiving roles
     * @param configuration resolved role configuration
     */
    private void assignRoles(MafiaMatch match, MafiaRoleConfiguration configuration) {
        List<MafiaPlayer> players = new ArrayList<>(match.getPlayers());
        List<MafiaRole> roles = new ArrayList<>(matchEngine.createRoleDeck(players.size(), configuration));
        Collections.shuffle(roles);

        for (int i = 0; i < players.size(); i++) {
            players.get(i).setRole(roles.get(i));
            gameLogger.log(match, MafiaEventType.ROLE_ASSIGNED,
                    "Role assigned to player.",
                    null, players.get(i).getUserId(), match.getMainChannelId());
        }
    }

    /**
     * Creates one private role thread under the main channel.
     *
     * @param mainChannel main event text channel
     * @param role role that owns the thread
     * @return thread creation action
     */
    private ThreadChannelAction createPrivateThread(TextChannel mainChannel, MafiaRole role) {
        return mainChannel.createThreadChannel(role.getPrivateThreadName(), true)
                .setInvitable(false);
    }

    /**
     * Builds thread-member add actions for every role-specific participant.
     *
     * @param match active match
     * @param assassinThread assassins thread
     * @param doctorThread doctors thread
     * @param detectiveThread detectives thread
     * @return actions that add members to their role threads
     */
    private List<RestAction<?>> buildThreadMemberActions(MafiaMatch match, ThreadChannel assassinThread,
                                                         ThreadChannel doctorThread, ThreadChannel detectiveThread) {
        List<RestAction<?>> actions = new ArrayList<>();

        for (MafiaPlayer player : match.getPlayers()) {
            if (player.getRole() == MafiaRole.ASSASSIN) {
                actions.add(assassinThread.addThreadMemberById(player.getUserId()));
            } else if (player.getRole() == MafiaRole.DOCTOR) {
                actions.add(doctorThread.addThreadMemberById(player.getUserId()));
            } else if (player.getRole() == MafiaRole.DETECTIVE) {
                actions.add(detectiveThread.addThreadMemberById(player.getUserId()));
            }
        }

        return actions;
    }

    /**
     * Sends the current night prompt and menu to each private role thread.
     *
     * @param match active match
     * @param guild guild that owns the match
     * @param refreshed whether this is a refresh caused by roster changes
     */
    private void sendNightMenus(MafiaMatch match, Guild guild, boolean refreshed) {
        for (MafiaRole role : List.of(MafiaRole.ASSASSIN, MafiaRole.DOCTOR, MafiaRole.DETECTIVE)) {
            ThreadChannel thread = getPrivateThread(guild, match, role);
            if (thread == null) {
                continue;
            }

            thread.sendMessageEmbeds(
                            refreshed
                                    ? MafiaMessageFactory.createNightPromptRefresh(match, role)
                                    : MafiaMessageFactory.createNightPrompt(match, role)
                    )
                    .setComponents(ActionRow.of(MafiaComponentFactory.createNightVoteMenu(guild, match, role)))
                    .queue();
        }
    }

    /**
     * Sends the day vote embed and vote menu to the main channel.
     *
     * @param match active match
     * @param guild guild that owns the match
     * @param refreshed whether this is a refresh caused by roster changes
     */
    private void sendDayVoteMessage(MafiaMatch match, Guild guild, boolean refreshed) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null) {
            return;
        }

        mainChannel.sendMessageEmbeds(
                        refreshed
                                ? MafiaMessageFactory.createDayVoteRefresh(match)
                                : MafiaMessageFactory.createDayVote(match)
                )
                .setComponents(
                        ActionRow.of(MafiaComponentFactory.createDayVoteMenu(guild, match)),
                        ActionRow.of(
                                MafiaComponentFactory.createResolveDayVoteButton(),
                                MafiaComponentFactory.createViewRoleButton()
                        )
                )
                .queue();
    }

    /**
     * Attempts to notify the host that the main channel was deleted and the match had to be terminated.
     *
     * @param api JDA instance used to open the DM
     * @param match terminated match
     * @param deletedChannelId deleted main channel id
     */
    private void notifyHostAboutMainChannelDeletion(JDA api, MafiaMatch match, long deletedChannelId) {
        api.retrieveUserById(match.getHostId())
                .flatMap(user -> user.openPrivateChannel()
                        .flatMap(channel -> channel.sendMessageEmbeds(
                                MafiaMessageFactory.createChannelDeletedTermination(deletedChannelId, true)
                        )))
                .queue(
                        success -> {},
                        error -> LOGGER.warn("Could not notify Oficina Dorme host {} about deleted main channel {}",
                                match.getHostId(), deletedChannelId, error)
                );
    }

    /**
     * Removes a player from one specific match and handles any follow-up effects caused by that departure.
     *
     * @param match affected match
     * @param guild guild that owns the match
     * @param userId removed user id
     * @param reason localized explanation for the removal
     */
    private void handlePlayerUnavailable(MafiaMatch match, Guild guild, long userId, String reason) {
        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());

        MafiaRole removedRole;
        boolean removedAlive;
        boolean refreshLobby = false;
        boolean refreshNightMenus = false;
        boolean refreshDayVote = false;
        boolean lastAssassinDeparted = false;
        NightResolution nightResolution = null;
        Optional<MafiaTeam> winner = Optional.empty();

        synchronized (match) {
            MafiaPlayer removedPlayer = match.getPlayer(userId);
            if (removedPlayer == null) {
                return;
            }

            removedRole = removedPlayer.getRole();
            removedAlive = removedPlayer.isAlive();
            int aliveAssassinsBeforeRemoval = match.getAlivePlayersByRole(MafiaRole.ASSASSIN).size();

            if (!match.removePlayer(userId)) {
                return;
            }

            if (match.getPhase() == MafiaPhase.LOBBY) {
                refreshLobby = true;
            } else if (match.getPhase() != MafiaPhase.ENDED) {
                lastAssassinDeparted = removedAlive
                        && removedRole == MafiaRole.ASSASSIN
                        && aliveAssassinsBeforeRemoval == 1;

                winner = matchEngine.determineWinner(match.getPlayers());
                if (winner.isPresent()) {
                    match.endMatch();
                } else if (match.getPhase() == MafiaPhase.NIGHT) {
                    if (match.hasAllNightActionsSubmitted()) {
                        nightResolution = matchEngine.resolveNight(match);
                        winner = matchEngine.determineWinner(match.getPlayers());
                        if (winner.isPresent()) {
                            match.endMatch();
                        } else {
                            match.startDayDiscussionPhase();
                        }
                    } else {
                        refreshNightMenus = true;
                    }
                } else if (match.getPhase() == MafiaPhase.DAY_VOTING) {
                    refreshDayVote = true;
                }
            }
        }

        if (mainChannel != null) {
            gameLogger.log(match, MafiaEventType.PLAYER_UNAVAILABLE,
                    "Player became unavailable and was removed from the match.",
                    null, userId, match.getMainChannelId());
            mainChannel.sendMessageEmbeds(MafiaMessageFactory.createPlayerUnavailableNotice(userId, reason)).queue();
        }

        if (refreshLobby) {
            refreshLobbyMessage(guild, match);
            return;
        }

        if (nightResolution != null) {
            announceDetectiveResults(match, guild, nightResolution);

            if (winner.isPresent()) {
                if (mainChannel != null) {
                    mainChannel.sendMessageEmbeds(
                            MafiaMessageFactory.createDepartureVictoryNotice(winner.get(), lastAssassinDeparted)
                    ).queue();
                }

                announceGameOver(match, guild, winner.get());
                endMatch(match.getMainChannelId());
                return;
            }

            announceDayDiscussion(match, guild, nightResolution);
            return;
        }

        if (winner.isPresent()) {
            if (mainChannel != null) {
                mainChannel.sendMessageEmbeds(
                        MafiaMessageFactory.createDepartureVictoryNotice(winner.get(), lastAssassinDeparted)
                ).queue();
            }
            gameLogger.log(match, MafiaEventType.GAME_TERMINATED,
                    lastAssassinDeparted
                            ? "Game terminated because the last assassin became unavailable."
                            : "Game terminated because player unavailability changed the victory state.",
                    null, userId, match.getMainChannelId());

            announceGameOver(match, guild, winner.get());
            endMatch(match.getMainChannelId());
            return;
        }

        if (refreshNightMenus) {
            announceNightMenuRefresh(match, guild);
        } else if (refreshDayVote) {
            announceDayVoteRefresh(match, guild);
        }
    }
}
