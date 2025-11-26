package ofc.bot.listeners.discord.interactions.buttons.groups;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.handlers.economy.*;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.GroupHelper;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InteractionHandler(scope = Scopes.Group.PIN_MESSAGE, autoResponseType = AutoResponseType.THINKING)
public class GroupPinsHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupPinsHandler.class);
    private static final BetManager betManager = BetManager.getManager();

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        OficinaGroup group = ctx.get("group");
        CurrencyType currency = group.getCurrency();
        PaymentManager bank = PaymentManagerProvider.fromType(currency);
        TextChannel chan = group.getTextChannel();
        boolean shouldPin = ctx.get("is_pin");
        long msgId = ctx.get("message_id");
        long guildId = ctx.getGuildId();
        long ownerId = group.getOwnerId();
        int price = ctx.get("amount");

        if (betManager.isBetting(ownerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        if (chan == null)
            return Status.YOUR_GROUP_DOES_NOT_HAVE_TEXT_CHANNEL;

        Message msg;
        try {
            msg = chan.retrieveMessageById(msgId).complete();
        } catch (Exception e) {
            return parseException(e);
        }

        boolean isPinned = msg.isPinned();
        if (isPinned && shouldPin)
            return Status.MESSAGE_ALREADY_PINNED;

        if (!isPinned && !shouldPin)
            return Status.MESSAGE_ALREADY_UNPINNED;

        BankAction chargeAction = bank.charge(ownerId, guildId, 0, price, "Pin Group Message");
        if (!chargeAction.isOk())
            return Status.INSUFFICIENT_BALANCE;

        getAction(msg, shouldPin).queue(v -> {
            if (shouldPin) {
                ctx.reply(Status.MESSAGE_SUCCESSFULLY_PINNED);
                GroupHelper.registerMessagePinned(group, price);
            } else
                ctx.reply(Status.MESSAGE_SUCCESSFULLY_UNPINNED);
        }, err -> {
            if (err instanceof ErrorResponseException errResp &&
                    errResp.getErrorResponse() == ErrorResponse.MAX_MESSAGE_PINS) {
                ctx.reply(Status.HIT_MAX_PINNED_MESSAGES);
                return;
            }

            LOGGER.error("Could not pin message", err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        ctx.disable();
        return Status.OK;
    }

    private RestAction<?> getAction(Message msg, boolean shouldPin) {
        return shouldPin ? msg.pin() : msg.unpin();
    }

    private Status parseException(Exception e) {
        if (!(e instanceof ErrorResponseException erresp)) {
            LOGGER.error("Could not find message to be pinned", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }

        return switch (erresp.getErrorResponse()) {
            case UNKNOWN_CHANNEL -> Status.CHANNEL_NOT_FOUND;
            case UNKNOWN_MESSAGE -> Status.MESSAGE_NOT_FOUND;
            default -> Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        };
    }
}
