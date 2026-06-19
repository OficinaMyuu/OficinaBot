package ofc.bot.handlers.games.betting.blackjack;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import ofc.bot.handlers.interactions.InteractionMemoryManager;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonContext;
import ofc.bot.util.Scopes;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BlackjackComponentFactory {
    private static final InteractionMemoryManager INTERACTION_MANAGER = InteractionMemoryManager.getManager();

    private BlackjackComponentFactory() {}

    public static List<ActionRow> active(BlackjackGame game) {
        ButtonContext hit = ButtonContext.primary("Pedir Carta")
                .setScope(Scopes.Bets.BLACKJACK_GAME)
                .setValidity(BlackjackGame.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .addUser(game.userId())
                .put("game_id", game.id())
                .put("game", game)
                .put("action", BlackjackAction.HIT)
                .setEnabled(game.canHit());

        ButtonContext stand = ButtonContext.success("Parar")
                .setScope(Scopes.Bets.BLACKJACK_GAME)
                .setValidity(BlackjackGame.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .addUser(game.userId())
                .put("game_id", game.id())
                .put("game", game)
                .put("action", BlackjackAction.STAND)
                .setEnabled(game.canStand());

        ButtonContext doubleDown = ButtonContext.secondary("Dobrar")
                .setScope(Scopes.Bets.BLACKJACK_GAME)
                .setValidity(BlackjackGame.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .addUser(game.userId())
                .put("game_id", game.id())
                .put("game", game)
                .put("action", BlackjackAction.DOUBLE_DOWN)
                .setEnabled(game.canDoubleDown());

        ButtonContext split = ButtonContext.secondary("Dividir")
                .setScope(Scopes.Bets.BLACKJACK_GAME)
                .setValidity(BlackjackGame.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .addUser(game.userId())
                .put("game_id", game.id())
                .put("game", game)
                .put("action", BlackjackAction.SPLIT)
                .setEnabled(game.canSplit());

        INTERACTION_MANAGER.save(hit, stand, doubleDown, split);
        return List.of(ActionRow.of(hit.getEntity(), stand.getEntity(), doubleDown.getEntity(), split.getEntity()));
    }

    public static List<ActionRow> finished() {
        return List.of(ActionRow.of(
                Button.primary("blackjack:disabled:hit", "Pedir Carta").asDisabled(),
                Button.success("blackjack:disabled:stand", "Parar").asDisabled(),
                Button.secondary("blackjack:disabled:double", "Dobrar").asDisabled(),
                Button.secondary("blackjack:disabled:split", "Dividir").asDisabled()
        ));
    }
}
