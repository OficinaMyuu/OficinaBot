package ofc.bot.listeners.discord.interactions.buttons.bets;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import ofc.bot.Main;
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.games.GameArgs;
import ofc.bot.handlers.games.betting.tictactoe.TicTacToeGame;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;

@InteractionHandler(scope = Scopes.Bets.CREATE_TICTACTOE, autoResponseType = AutoResponseType.DEFER_EDIT)
public class TicTacToeAcceptHandler implements InteractionListener<ButtonClickContext> {
    private static final BetManager betManager = BetManager.getManager();
    private final UserEconomyRepository ecoRepo;
    private final BetGameRepository betRepo;
    private final GameParticipantRepository betUsersRepo;
    private final AppUserBanRepository appBanRepo;

    public TicTacToeAcceptHandler(UserEconomyRepository ecoRepo, BetGameRepository betRepo,
                                  GameParticipantRepository betUsersRepo, AppUserBanRepository appBanRepo) {
        this.ecoRepo = ecoRepo;
        this.betRepo = betRepo;
        this.betUsersRepo = betUsersRepo;
        this.appBanRepo = appBanRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int amount = ctx.get("amount");
        long authorId = ctx.get("author_id");
        long userId = ctx.getUserId();
        JDA api = Main.getApi();

        var error = checks(userId, authorId, amount);
        if (error != null) return error;

        Message msg = ctx.getMessage();
        TicTacToeGame game = new TicTacToeGame(ecoRepo, betRepo, betUsersRepo, appBanRepo, api, amount);
        game.join(authorId, userId);

        game.start(new GameArgs(msg));
        return Status.OK;
    }

    private InteractionResult checks(long userId, long authorId, int amount) {
        if (betManager.isBetting(authorId)) return Status.MEMBER_IS_BETTING_PLEASE_WAIT.args(authorId);
        if (betManager.isBetting(userId)) return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        if (!ecoRepo.hasEnoughBank(authorId, amount)) return Status.MEMBER_INSUFFICIENT_BALANCE.args(authorId);
        if (!ecoRepo.hasEnoughBank(userId, amount)) return Status.INSUFFICIENT_BALANCE;
        return null;
    }
}