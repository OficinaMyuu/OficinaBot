package ofc.bot.listeners.discord.interactions.buttons.mafia;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.handlers.games.mafia.discord.MafiaComponentFactory;
import ofc.bot.handlers.games.mafia.discord.MafiaMessageFactory;
import ofc.bot.handlers.games.mafia.domain.DayResolution;
import ofc.bot.handlers.games.mafia.domain.MafiaMatch;
import ofc.bot.handlers.games.mafia.domain.MafiaPlayer;
import ofc.bot.handlers.games.mafia.domain.NightResolution;
import ofc.bot.handlers.games.mafia.enums.MafiaEventType;
import ofc.bot.handlers.games.mafia.enums.MafiaPhase;
import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import ofc.bot.handlers.games.mafia.enums.MafiaTeam;
import ofc.bot.handlers.games.mafia.service.MafiaGameLogger;
import ofc.bot.handlers.games.mafia.service.MafiaGameManager;
import ofc.bot.handlers.games.mafia.service.MafiaMatchEngine;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.Optional;

/**
 * Handles all button and select-menu interactions for Oficina Dorme.
 * <p>
 * This listener only deals with explicit player actions. Passive lifecycle events such as members leaving the guild
 * or channels being deleted are handled separately by {@code MafiaLifecycleListener}.
 */
@DiscordEventHandler
public class MafiaInteractionListener extends ListenerAdapter {
    private final MafiaGameManager gameManager = MafiaGameManager.getInstance();
    private final MafiaGameLogger gameLogger = MafiaGameLogger.getInstance();
    private final MafiaMatchEngine matchEngine = gameManager.getMatchEngine();

    /**
     * Routes every mafia button interaction to its dedicated handler.
     *
     * @param event Discord button interaction
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (!buttonId.startsWith("mafia_")) {
            return;
        }

        switch (buttonId) {
            case MafiaComponentFactory.LOBBY_JOIN_BUTTON_ID -> handleLobbyJoin(event);
            case MafiaComponentFactory.LOBBY_LEAVE_BUTTON_ID -> handleLobbyLeave(event);
            case MafiaComponentFactory.LOBBY_START_BUTTON_ID -> handleLobbyStart(event);
            case MafiaComponentFactory.VIEW_ROLE_BUTTON_ID -> handleViewRole(event);
            case MafiaComponentFactory.OPEN_DAY_VOTE_BUTTON_ID -> handleOpenDayVote(event);
            case MafiaComponentFactory.RESOLVE_DAY_VOTE_BUTTON_ID -> handleResolveDayVote(event);
            default -> {
            }
        }
    }

    /**
     * Routes every mafia select-menu interaction to its dedicated handler.
     *
     * @param event Discord select-menu interaction
     */
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("mafia_")) {
            return;
        }

        switch (componentId) {
            case MafiaComponentFactory.ASSASSIN_MENU_ID -> handleNightVote(event, MafiaRole.ASSASSIN);
            case MafiaComponentFactory.DOCTOR_MENU_ID -> handleNightVote(event, MafiaRole.DOCTOR);
            case MafiaComponentFactory.DETECTIVE_MENU_ID -> handleNightVote(event, MafiaRole.DETECTIVE);
            case MafiaComponentFactory.DAY_VOTE_MENU_ID -> handleDayVote(event);
            default -> {
            }
        }
    }

    /**
     * Adds one user to the lobby roster through the join button.
     *
     * @param event join-button interaction
     */
    private void handleLobbyJoin(ButtonInteractionEvent event) {
        MafiaMatch match = gameManager.getMatchByMainChannel(event.getChannelIdLong());
        if (match == null || match.getPhase() != MafiaPhase.LOBBY) {
            event.reply("Esta partida já começou ou foi encerrada.").setEphemeral(true).queue();
            return;
        }

        if (event.getUser().isBot()) {
            event.reply("Bots não podem participar desta partida.").setEphemeral(true).queue();
            return;
        }

        String reply;
        synchronized (match) {
            if (match.hasPlayer(event.getUser().getIdLong())) {
                reply = "Você já está participando da partida.";
            } else if (match.isFull()) {
                reply = "O lobby já atingiu o limite máximo de jogadores.";
            } else {
                match.addPlayer(event.getUser().getIdLong());
                reply = "Você entrou na partida.";
                gameLogger.log(match, MafiaEventType.PLAYER_JOINED, "Player joined the lobby.",
                        event.getUser().getIdLong(), null, event.getChannelIdLong());
            }
        }

        event.deferReply(true).queue();
        event.getMessage().editMessageEmbeds(MafiaMessageFactory.createLobbyEmbed(match)).queue();
        event.getHook().editOriginal(reply).queue();
    }

    /**
     * Removes one user from the lobby roster through the leave button.
     *
     * @param event leave-button interaction
     */
    private void handleLobbyLeave(ButtonInteractionEvent event) {
        MafiaMatch match = gameManager.getMatchByMainChannel(event.getChannelIdLong());
        if (match == null || match.getPhase() != MafiaPhase.LOBBY) {
            event.reply("Esta partida já começou ou foi encerrada.").setEphemeral(true).queue();
            return;
        }

        String reply;
        synchronized (match) {
            if (!match.hasPlayer(event.getUser().getIdLong())) {
                reply = "Você não está participando desta partida.";
            } else {
                match.removePlayer(event.getUser().getIdLong());
                reply = "Você saiu da partida.";
                gameLogger.log(match, MafiaEventType.PLAYER_LEFT_LOBBY, "Player left the lobby.",
                        event.getUser().getIdLong(), null, event.getChannelIdLong());
            }
        }

        event.deferReply(true).queue();
        event.getMessage().editMessageEmbeds(MafiaMessageFactory.createLobbyEmbed(match)).queue();
        event.getHook().editOriginal(reply).queue();
    }

    /**
     * Starts the lobby when triggered by the host or by staff.
     *
     * @param event start-button interaction
     */
    private void handleLobbyStart(ButtonInteractionEvent event) {
        MafiaMatch match = gameManager.getMatchByMainChannel(event.getChannelIdLong());
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (match == null || guild == null || member == null) {
            event.reply("Esta partida não está mais disponível.").setEphemeral(true).queue();
            return;
        }

        if (match.getPhase() != MafiaPhase.LOBBY) {
            event.reply("A partida já começou.").setEphemeral(true).queue();
            return;
        }

        if (!isHostOrStaff(match, member)) {
            event.reply("Apenas o host ou a staff pode começar a partida.").setEphemeral(true).queue();
            return;
        }

        if (match.getPlayerCount() < MafiaMatch.MIN_PLAYERS) {
            event.reply("São necessários pelo menos 6 jogadores para começar.").setEphemeral(true).queue();
            return;
        }

        gameLogger.log(match, MafiaEventType.GAME_STARTED, "Host started the match.",
                event.getUser().getIdLong(), null, event.getChannelIdLong());
        event.deferReply(true).queue();
        gameManager.startMatch(match, guild, event.getHook());
    }

    /**
     * Privately reveals the participant's own role.
     *
     * @param event role-reveal button interaction
     */
    private void handleViewRole(ButtonInteractionEvent event) {
        MafiaMatch match = gameManager.getMatch(event.getChannelIdLong());
        if (match == null) {
            event.reply("Esta partida não está mais disponível.").setEphemeral(true).queue();
            return;
        }

        MafiaPlayer player = match.getPlayer(event.getUser().getIdLong());
        if (player == null) {
            event.reply("Você não faz parte desta partida.").setEphemeral(true).queue();
            return;
        }

        if (player.getRole() == null) {
            event.reply("As funções ainda não foram distribuídas.").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(MafiaMessageFactory.createRoleRevealEmbed(player.getRole()))
                .setEphemeral(true)
                .queue();
    }

    /**
     * Opens day voting after the discussion phase.
     *
     * @param event open-day-vote button interaction
     */
    private void handleOpenDayVote(ButtonInteractionEvent event) {
        MafiaMatch match = gameManager.getMatchByMainChannel(event.getChannelIdLong());
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (match == null || guild == null || member == null) {
            event.reply("Esta partida não está mais disponível.").setEphemeral(true).queue();
            return;
        }

        synchronized (match) {
            if (match.getPhase() != MafiaPhase.DAY_DISCUSSION) {
                event.reply("A votação do dia não pode ser aberta agora.").setEphemeral(true).queue();
                return;
            }

            if (!isHostOrStaff(match, member)) {
                event.reply("Apenas o host ou a staff pode abrir a votação do dia.").setEphemeral(true).queue();
                return;
            }

            match.startDayVotingPhase();
        }

        event.reply("A votação do dia foi aberta.").setEphemeral(true).queue();
        gameLogger.log(match, MafiaEventType.DAY_VOTE_OPENED, "Day vote opened.",
                event.getUser().getIdLong(), null, event.getChannelIdLong());
        gameManager.announceDayVote(match, guild);
    }

    /**
     * Resolves the currently open day vote.
     *
     * @param event resolve-day-vote button interaction
     */
    private void handleResolveDayVote(ButtonInteractionEvent event) {
        MafiaMatch match = gameManager.getMatchByMainChannel(event.getChannelIdLong());
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (match == null || guild == null || member == null) {
            event.reply("Esta partida não está mais disponível.").setEphemeral(true).queue();
            return;
        }

        DayResolution resolution;
        Optional<MafiaTeam> winner;

        synchronized (match) {
            if (match.getPhase() != MafiaPhase.DAY_VOTING) {
                event.reply("A votação do dia não está aberta agora.").setEphemeral(true).queue();
                return;
            }

            if (!isHostOrStaff(match, member)) {
                event.reply("Apenas o host ou a staff pode encerrar a votação do dia.").setEphemeral(true).queue();
                return;
            }

            resolution = matchEngine.resolveDay(match.getDayVotes());
            if (resolution.eliminatedPlayerId() != null) {
                MafiaPlayer eliminatedPlayer = match.getPlayer(resolution.eliminatedPlayerId());
                if (eliminatedPlayer != null) {
                    eliminatedPlayer.setAlive(false);
                }
            }

            winner = matchEngine.determineWinner(match.getPlayers());
            if (winner.isPresent()) {
                match.endMatch();
            } else {
                match.startNightPhase();
            }
        }

        event.reply("A votação do dia foi encerrada.").setEphemeral(true).queue();
        gameLogger.log(match, MafiaEventType.DAY_VOTE_RESOLVED,
                resolution.tie() ? "Day vote ended in a tie." : "Day vote resolved.",
                event.getUser().getIdLong(), resolution.eliminatedPlayerId(), event.getChannelIdLong());
        if (resolution.eliminatedPlayerId() != null) {
            gameLogger.log(match, MafiaEventType.PLAYER_ELIMINATED, "Player was eliminated by day vote.",
                    event.getUser().getIdLong(), resolution.eliminatedPlayerId(), event.getChannelIdLong());
        }
        gameManager.announceDayResolution(match, guild, resolution);

        if (winner.isPresent()) {
            gameManager.announceGameOver(match, guild, winner.get());
            gameManager.endMatch(match.getMainChannelId());
            return;
        }

        gameLogger.log(match, MafiaEventType.NIGHT_STARTED, "Night phase started.", null, null, match.getMainChannelId());
        gameManager.announceNightPhase(match, guild);
    }

    /**
     * Records one night action inside the corresponding private role thread.
     *
     * @param event select-menu interaction
     * @param actingRole role expected to act in that thread
     */
    private void handleNightVote(StringSelectInteractionEvent event, MafiaRole actingRole) {
        MafiaMatch match = gameManager.getMatch(event.getChannelIdLong());
        Guild guild = event.getGuild();
        if (match == null || guild == null) {
            event.reply("Esta partida não está mais disponível.").setEphemeral(true).queue();
            return;
        }

        long actorId = event.getUser().getIdLong();
        long targetId;
        try {
            targetId = Long.parseLong(event.getValues().getFirst());
        } catch (NumberFormatException exception) {
            event.reply("Não foi possível entender o jogador selecionado.").setEphemeral(true).queue();
            return;
        }

        String reply;
        NightResolution resolution = null;
        Optional<MafiaTeam> winner = Optional.empty();

        synchronized (match) {
            if (match.getPhase() != MafiaPhase.NIGHT) {
                event.reply("A noite atual já foi encerrada.").setEphemeral(true).queue();
                return;
            }

            MafiaPlayer actor = match.getPlayer(actorId);
            MafiaPlayer target = match.getPlayer(targetId);
            Long roleThreadId = match.getPrivateThreadId(actingRole);

            if (roleThreadId == null || roleThreadId != event.getChannelIdLong()) {
                event.reply("Esta ação só pode ser feita na thread correta da função.").setEphemeral(true).queue();
                return;
            }

            if (actor == null || !actor.isAlive() || actor.getRole() != actingRole) {
                event.reply("Você não pode agir nesta rodada.").setEphemeral(true).queue();
                return;
            }

            if (target == null || !target.isAlive()) {
                event.reply("O jogador selecionado não está disponível para esta rodada.").setEphemeral(true).queue();
                return;
            }

            if (match.hasSubmittedNightVote(actingRole, actorId)) {
                event.reply("Você já escolheu seu alvo nesta noite.").setEphemeral(true).queue();
                return;
            }

            if (actorId == targetId && actingRole != MafiaRole.DOCTOR) {
                event.reply("Você não pode escolher a si mesmo nesta ação.").setEphemeral(true).queue();
                return;
            }

            if (actingRole == MafiaRole.ASSASSIN && target.getRole() == MafiaRole.ASSASSIN) {
                event.reply("Assassinos não podem eliminar alguém do próprio time.").setEphemeral(true).queue();
                return;
            }

            if ((actingRole == MafiaRole.DOCTOR || actingRole == MafiaRole.DETECTIVE)
                    && actor.getPreviousNightTargetId() != null
                    && actor.getPreviousNightTargetId() == targetId) {
                event.reply("Você não pode escolher a mesma pessoa em duas noites seguidas.").setEphemeral(true).queue();
                return;
            }

            switch (actingRole) {
                case ASSASSIN -> match.getAssassinVotes().put(actorId, targetId);
                case DOCTOR -> {
                    match.getDoctorVotes().put(actorId, targetId);
                    actor.setPreviousNightTargetId(targetId);
                }
                case DETECTIVE -> {
                    match.getDetectiveVotes().put(actorId, targetId);
                    actor.setPreviousNightTargetId(targetId);
                }
                case VILLAGER -> {
                }
            }
            gameLogger.log(match, MafiaEventType.NIGHT_ACTION_SUBMITTED,
                    actingRole.name() + " submitted a night action.",
                    actorId, targetId, event.getChannelIdLong());

            reply = switch (actingRole) {
                case ASSASSIN -> "Seu alvo da noite foi registrado.";
                case DOCTOR -> "Sua proteção da noite foi registrada.";
                case DETECTIVE -> "Sua investigação da noite foi registrada.";
                case VILLAGER -> "Ação registrada.";
            };

            if (match.hasAllNightActionsSubmitted()) {
                resolution = matchEngine.resolveNight(match);
                winner = matchEngine.determineWinner(match.getPlayers());
                if (winner.isPresent()) {
                    match.endMatch();
                } else {
                    match.startDayDiscussionPhase();
                }
            }
        }

        event.reply(reply).setEphemeral(true).queue();

        if (resolution == null) {
            return;
        }

        gameManager.announceDetectiveResults(match, guild, resolution);

        if (winner.isPresent()) {
            gameManager.announceGameOver(match, guild, winner.get());
            gameManager.endMatch(match.getMainChannelId());
            return;
        }

        gameManager.announceDayDiscussion(match, guild, resolution);
    }

    /**
     * Records one public day vote in the main channel.
     *
     * @param event day-vote select-menu interaction
     */
    private void handleDayVote(StringSelectInteractionEvent event) {
        MafiaMatch match = gameManager.getMatchByMainChannel(event.getChannelIdLong());
        if (match == null) {
            event.reply("Esta partida não está mais disponível.").setEphemeral(true).queue();
            return;
        }

        long voterId = event.getUser().getIdLong();
        long targetId;
        try {
            targetId = Long.parseLong(event.getValues().getFirst());
        } catch (NumberFormatException exception) {
            event.reply("Não foi possível entender o jogador selecionado.").setEphemeral(true).queue();
            return;
        }

        synchronized (match) {
            if (match.getPhase() != MafiaPhase.DAY_VOTING) {
                event.reply("A votação do dia não está aberta agora.").setEphemeral(true).queue();
                return;
            }

            MafiaPlayer voter = match.getPlayer(voterId);
            MafiaPlayer target = match.getPlayer(targetId);

            if (voter == null || !voter.isAlive()) {
                event.reply("Você não pode votar nesta rodada.").setEphemeral(true).queue();
                return;
            }

            if (target == null || !target.isAlive()) {
                event.reply("O jogador selecionado não pode receber votos agora.").setEphemeral(true).queue();
                return;
            }

            if (voterId == targetId) {
                event.reply("Você não pode votar em si mesmo.").setEphemeral(true).queue();
                return;
            }

            if (match.hasSubmittedDayVote(voterId)) {
                event.reply("Você já votou nesta rodada.").setEphemeral(true).queue();
                return;
            }

            match.getDayVotes().put(voterId, targetId);
            gameLogger.log(match, MafiaEventType.DAY_VOTE_SUBMITTED, "Player submitted a day vote.",
                    voterId, targetId, event.getChannelIdLong());
        }

        event.reply("Seu voto do dia foi registrado.").setEphemeral(true).queue();
    }

    /**
     * Checks whether the interacting member is the match host or has the required staff permission.
     *
     * @param match active match
     * @param member interacting guild member
     * @return {@code true} when the member can operate host-only controls
     */
    private boolean isHostOrStaff(MafiaMatch match, Member member) {
        return member.getIdLong() == match.getHostId() || member.hasPermission(Permission.MANAGE_SERVER);
    }
}
