package ofc.bot.handlers.games.betting.tictactoe;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.entity.*;
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.handlers.games.GameArgs;
import ofc.bot.handlers.games.GameStatus;
import ofc.bot.handlers.games.GameType;
import ofc.bot.handlers.games.betting.Bet;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.games.betting.exceptions.BetGameCreationException;
import ofc.bot.handlers.interactions.*;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import ofc.bot.util.time.ElasticScheduler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class TicTacToeGame implements Bet<Character> {
    public static final int MIN_AMOUNT = 100;
    public static final int MAX_AMOUNT = 10_000;
    public static final int DEFAULT_GRID_SIZE = 3;
    public static final int TIMEOUT = 60 * 1000;
    public static final float TIMEOUT_PENALTY_RATE = 1.25f; // Yes, 125%
    private static final Logger LOGGER = LoggerFactory.getLogger(TicTacToeGame.class);
    private static final ErrorHandler DEF_ERR_HANDLER = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);
    private static final int REQUIRED_PLAYERS = 2;
    private static final InteractionMemoryManager memoryManager = InteractionMemoryManager.getManager();
    private static final Random RANDOM = new Random();
    private static final BetManager betManager = BetManager.getManager();
    private final int prizeValue;
    private final long id;
    private final UserEconomyRepository ecoRepo;
    private final BetGameRepository betRepo;
    private final GameParticipantRepository betUsersRepo;
    private final AppUserBanRepository appBanRepo;
    private final JDA api;
    private final GameGrid grid;
    private final Map<Long, Character> players;
    private final ElasticScheduler scheduler; // Handle timeout system
    private long winnerId;
    private long currentPlayerId;
    private long startedAt;
    private Message message;
    private GameHandler gameHandler;
    private MessageDeleteHandler messageDeleteHandler;
    private GameStatus status;

    public TicTacToeGame(UserEconomyRepository ecoRepo, BetGameRepository betRepo,
                         GameParticipantRepository betUsersRepo, AppUserBanRepository appBanRepo,
                         JDA api, int prizeValue, int gridSize) {
        Checks.notNull(ecoRepo, "Economy Repository");
        Checks.notNull(betRepo, "Bet Repository");
        Checks.notNull(betUsersRepo, "Bet Users Repository");
        Checks.notNull(api, "JDA");

        if (prizeValue < MIN_AMOUNT || prizeValue > MAX_AMOUNT)
            failRange(prizeValue);

        this.prizeValue = prizeValue;
        this.id = System.nanoTime();
        this.ecoRepo = ecoRepo;
        this.betRepo = betRepo;
        this.betUsersRepo = betUsersRepo;
        this.appBanRepo = appBanRepo;
        this.api = api;
        this.grid = new GameGrid(gridSize);
        this.players = new HashMap<>(REQUIRED_PLAYERS);
        this.status = GameStatus.WAITING;
        this.scheduler = new ElasticScheduler(this::timeout, TIMEOUT);
        this.winnerId = 0;
    }

    public TicTacToeGame(UserEconomyRepository ecoRepo, BetGameRepository betRepo,
                         GameParticipantRepository betUsersRepo, AppUserBanRepository appBanRepo,
                         JDA api, int prizeValue) {
        this(ecoRepo, betRepo, betUsersRepo, appBanRepo, api, prizeValue, DEFAULT_GRID_SIZE);
    }

    @Override
    public void start(GameArgs args) {
        if (this.status == GameStatus.RUNNING) return;

        if (players.size() != REQUIRED_PLAYERS)
            failPlayers();

        this.message = args.get(0);
        this.startedAt = Bot.unixNow();
        this.status = GameStatus.RUNNING;
        this.gameHandler = new GameHandler();
        this.messageDeleteHandler = new MessageDeleteHandler(appBanRepo);
        this.currentPlayerId = pickRandomPlayer();

        betManager.addBets(this, players.keySet());
        memoryManager.registerListeners(this.gameHandler);
        api.addEventListener(this.messageDeleteHandler);

        api.retrieveUserById(currentPlayerId).queue(user -> {
            MessageEmbed embed = EmbedFactory.embedTicTacToeGame(user);
            List<ActionRow> rows = Stream.of(EntityContextFactory.createTicTacToeTable(id, currentPlayerId, grid))
                    .map(btns -> ActionRow.of(List.of(btns)))
                    .toList();

            message.editMessageEmbeds(embed)
                    .setComponents(rows)
                    .setReplace(true)
                    .queue((v) -> this.scheduler.start(), DEF_ERR_HANDLER);
        }, (err) -> LOGGER.error("Could not find user for ID {}", currentPlayerId, err));
    }

    @Override
    public void end(GameArgs args) {
        if (this.status != GameStatus.RUNNING) return;

        boolean isDraw = args.get(0);
        this.status = isDraw ? GameStatus.DRAW : GameStatus.COMPLETE;
        this.scheduler.shutdown();

        long timeEnded = Bot.unixNow();
        GameStatus exitStatus = resolveExitStatus(isDraw);

        // Remove bets and listeners regardless of the result
        betManager.removeBets(players.keySet());
        memoryManager.removeListener(this.gameHandler);
        api.removeEventListener(this.messageDeleteHandler);

        finalizeGame(exitStatus, timeEnded);
    }

    @Override
    public void timeout() {
        if (this.status != GameStatus.RUNNING) return;

        this.status = GameStatus.TIMED_OUT;
        long timeEnded = Bot.unixNow();
        long targetId = this.currentPlayerId;

        betManager.removeBets(players.keySet());
        memoryManager.removeListener(this.gameHandler);
        api.removeEventListener(this.messageDeleteHandler);

        int penaltyAmount = Math.round(this.prizeValue * TIMEOUT_PENALTY_RATE);
        try {
            User target = api.retrieveUserById(targetId).complete();
            UserEconomy userEco = ecoRepo.findByUserId(targetId, UserEconomy.fromUserId(targetId))
                    .modifyBalance(0, -penaltyAmount)
                    .tickUpdate();

            ecoRepo.upsert(userEco);

            // Inform the users about the timeout action taken
            MessageEmbed embed = EmbedFactory.embedTicTacToeTimeout(penaltyAmount, target);
            message.editMessageEmbeds(embed)
                    .setReplace(true)
                    .queue();

            persist(this.status, timeEnded);
        } catch (DataAccessException e) {
            LOGGER.error("Could not handle tictactoe timeout database operations", e);
        } catch (ErrorResponseException e) {
            LOGGER.error("Could not fetch penalized user of ID {}", targetId, e);
        } finally {
            this.scheduler.shutdown();
        }
    }

    @Override
    public int getTimeout() {
        return TIMEOUT;
    }

    @Override
    public void join(long... userIds) {
        for (long userId : userIds) {
            if (players.isEmpty()) {
                players.put(userId, 'X');
            } else {
                players.put(userId, 'O');
            }
        }
    }

    @Override
    public void join(long userId, @NotNull Character bet) {
        Checks.notNull(bet, "Bet");
        Checks.check(bet == 'X' || bet == 'O', "Bet must be X or O, provided: %s", bet);

        players.put(userId, bet);
    }

    @Override
    public GameType getType() {
        return GameType.TIC_TAC_TOE;
    }

    @Override
    public GameStatus getStatus() {
        return this.status;
    }

    @Override
    public long getTimeStarted() {
        return this.startedAt;
    }

    @Override
    public Set<Long> getParticipants() {
        return this.players.keySet();
    }

    @Override
    public Set<Long> getWinners() {
        return winnerId == 0 ? Set.of() : Set.of(winnerId);
    }

    @Override
    public int getMaxUsers() {
        return REQUIRED_PLAYERS;
    }

    @Override
    public int getMinUsers() {
        return REQUIRED_PLAYERS;
    }

    private long findLoserId() {
        return players.keySet()
                .stream()
                .filter(id -> id != winnerId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Somehow we did not find the loser ID, what???"));
    }

    private GameStatus resolveExitStatus(boolean isDraw) {
        if (!isDraw && this.winnerId == 0) {
            return GameStatus.INTERRUPTED;
        }
        return this.status;
    }

    private void handlePayment(User winner, User loser) {
        long loserId = loser.getIdLong();

        ecoRepo.transferBank(loserId, winnerId, prizeValue);
    }

    private void finalizeGame(GameStatus exitStatus, long timeEnded) {
        long loserId = findLoserId();
        try {
            User winner = winnerId == 0 ? null : api.retrieveUserById(winnerId).complete();
            User loser = api.retrieveUserById(loserId).complete();

            MessageEmbed embed = EmbedFactory.embedTicTacToeEnd(winner);
            message.editMessageEmbeds(embed)
                    .setReplace(true)
                    .queue(null, DEF_ERR_HANDLER);

            persist(exitStatus, timeEnded);

            // Only handle payment if a winner exists and the game was not interrupted
            if (winner != null && exitStatus != GameStatus.INTERRUPTED) {
                handlePayment(winner, loser);
            }
        } catch (DataAccessException e) {
            LOGGER.error("Could not make transaction from {} to {} of {}", loserId, winnerId, prizeValue, e);
        } catch (ErrorResponseException e) {
            LOGGER.error("Could not fetch users {} or {}", winnerId, loserId, e);
        }
    }

    private void persist(GameStatus status, long endTime) {
        BetGame bet = new BetGame(id, status, grid.toString(), getType(), startedAt, endTime);
        List<GameParticipant> participants = players.keySet()
                .stream()
                .map(pId -> new GameParticipant(id, pId, pId == winnerId))
                .toList();

        try {
            betRepo.save(bet);
            betUsersRepo.bulkSave(participants);
        } catch (DataAccessException e) {
            LOGGER.error("Could not save bet game or participants to the database", e);
        }
    }

    private void swapPlayers() {
        for (long userId : players.keySet()) {
            if (currentPlayerId != userId) {
                currentPlayerId = userId;
                return;
            }
        }
    }

    private long pickRandomPlayer() {
        int rand = RANDOM.nextInt(players.size());
        Long[] ids = players.keySet().toArray(new Long[0]);

        return ids[rand];
    }

    @Contract("-> fail")
    private void failPlayers() {
        throw new BetGameCreationException(String.format("TicTacToe game must have exactly %d players, provided: %d",
                REQUIRED_PLAYERS, players.size()));
    }

    @Contract("_ -> fail")
    private void failRange(int value) {
        throw new IllegalArgumentException(String.format("Prize value should be in range of %d - %d, provided: %d",
                MIN_AMOUNT, MAX_AMOUNT, value));
    }

    @InteractionHandler(scope = Scopes.Bets.TICTACTOE_GAME, autoResponseType = AutoResponseType.DEFER_EDIT)
    private class GameHandler implements InteractionListener<ButtonClickContext> {

        @Override
        public InteractionResult onExecute(ButtonClickContext ctx) {
            // Safety purposes, do not proceed if the status is no longer set as RUNNING
            if (status != GameStatus.RUNNING) return Status.OK;

            int row = ctx.get("row");
            int col = ctx.get("col");
            char symbol = players.get(currentPlayerId);
            grid.set(row, col, symbol);

            // Checking for winners
            char winnerSymbol = grid.getWinner();
            winnerId = resolveWinner(winnerSymbol);
            if (winnerId != '\0') {
                end(new GameArgs(false));
                return Status.OK;
            }

            // If we don't have any winners and the table/grid
            // is already full, then we can end the game, its a tie.
            if (!grid.hasSlotAvailable()) {
                end(new GameArgs(true));
                return Status.OK;
            }

            // Proceed if its not tied and we have no winners
            swapPlayers();
            User curr = api.retrieveUserById(currentPlayerId).complete();
            MessageEmbed embed = EmbedFactory.embedTicTacToeGame(curr);
            List<ActionRow> rows = Stream.of(EntityContextFactory.createTicTacToeTable(id, currentPlayerId, grid))
                    .map(btns -> ActionRow.of(List.of(btns)))
                    .toList();

            scheduler.reset();
            return ctx.create()
                    .setEmbeds(embed)
                    .setComponents(rows)
                    .edit();
        }

        @Override
        public boolean validate(ButtonClickContext ctx) {
            return ((long) ctx.get("bet_id")) == id;
        }

        private long resolveWinner(char value) {
            for (long userId : players.keySet()) {
                if (players.get(userId).equals(value)) {
                    return userId;
                }
            }
            return '\0';
        }
    }

    @DiscordEventHandler
    public class MessageDeleteHandler extends ListenerAdapter {
        private final AppUserBanRepository appBanRepo;

        private MessageDeleteHandler(AppUserBanRepository appBanRepo) {
            this.appBanRepo = appBanRepo;
        }

        @Override
        public void onMessageDelete(MessageDeleteEvent e) {
            if (e.getMessageIdLong() != message.getIdLong()) return;

            Guild guild = e.getGuild();
            long originalAuthorId = message.getAuthor().getIdLong();
            guild.retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).limit(1).queue((entries) -> {
                AuditLogEntry entry = entries.isEmpty() ? null : entries.getFirst();

                if (entry == null) return;

                /*
                 * Discord does not provide information on who originally sent a deleted message,
                 * not even in an AuditLog entry.
                 * We can only determine who deleted the message (`authorId`) and the author
                 * of this message (`targetId`). We are limited to these two values.
                 *
                 * Since multiple messages from the same user (this bot) can be deleted
                 * in a very short period of time (like milliseconds or even nanoseconds),
                 * there's a chance we may retrieve data related to another message
                 * from the same user a few milliseconds later.
                 *
                 * The best we can do here is to verify whether the authors of both messages are the same.
                 * However, there is still a risk we must take.
                 */
                long delAuthorId = entry.getUserIdLong(); // The user who deleted the message
                long targetId = entry.getTargetIdLong(); // The author of the deleted message

                // Authors don't match, we fetched the wrong message :/
                // (or the bot deleted its own message lol)
                if (targetId != originalAuthorId) return;

                // As the authors match, we ban this user and terminate the match.
                // If we ban the wrong one, all they have to do is text me and ask for unban ^^
                end(new GameArgs(false));
                appBanUser(delAuthorId);
            });
        }

        private void appBanUser(long userId) {
            long validity = Bot.unixNow() + TimeUnit.DAYS.toSeconds(7);

            AppUserBan ban = new AppUserBan(userId, "Deleted a TicTacToe message", validity);
            try {
                appBanRepo.save(ban);
                sendWarning(userId);
            } catch (DataAccessException e) {
                LOGGER.error("Could not save application ban of user {}", userId, e);
            }
        }

        private void sendWarning(long userId) {
            MessageChannel chan = message.getChannel();

            try {
                User author = api.retrieveUserById(userId).complete();
                MessageEmbed embed = EmbedFactory.embedTicTacToeDeleted(author, message.getGuild());

                chan.sendMessage(author.getAsMention())
                        .setEmbeds(embed)
                        .queue();
            } catch (ErrorResponseException e) {
                // Well, its not our day :/
                LOGGER.error("Could not fetch the user who deleted the message ({})", userId, e);
            }
        }
    }
}