package ofc.bot.listeners.discord.interactions.buttons.bets;

import ofc.bot.handlers.games.betting.blackjack.BlackjackAction;
import ofc.bot.handlers.games.betting.blackjack.BlackjackGame;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;

@InteractionHandler(scope = Scopes.Bets.BLACKJACK_GAME, autoResponseType = AutoResponseType.DEFER_EDIT)
public class BlackjackActionHandler implements InteractionListener<ButtonClickContext> {
    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        BlackjackGame game = ctx.get("game");
        BlackjackAction action = ctx.get("action");
        long gameId = ctx.get("game_id");

        if (game.id() != gameId) {
            return Status.OK;
        }
        return game.handle(action, ctx);
    }
}
