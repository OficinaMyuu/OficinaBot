package ofc.bot.listeners.discord.interactions.buttons.groups;

import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.RentStatus;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.handlers.economy.*;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Bot;
import ofc.bot.util.GroupHelper;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import org.jooq.exception.DataAccessException;

@InteractionHandler(scope = Scopes.Group.PAY_INVOICE, autoResponseType = AutoResponseType.THINKING)
public class GroupInvoicePaymentHandler implements InteractionListener<ButtonClickContext> {
    private static final BetManager betManager = BetManager.getManager();
    private final OficinaGroupRepository grpRepo;

    public GroupInvoicePaymentHandler(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        OficinaGroup group = ctx.get("group");
        CurrencyType currency = group.getCurrency();
        PaymentManager bank = PaymentManagerProvider.fromType(currency);
        long guildId = ctx.getGuildId();
        long ownerId = ctx.getUserId();
        int value = ctx.get("amount");

        if (betManager.isBetting(ownerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        BankAction chargeAction = bank.charge(ownerId, guildId, 0, value, "Group invoice payment");
        if (!chargeAction.isOk())
            return Status.INSUFFICIENT_BALANCE;

        group.setInvoiceAmount(0)
                .setRentStatus(RentStatus.PAID)
                .tickUpdate();

        try {
            grpRepo.upsert(group);
            GroupHelper.registerInvoicePaid(group, value);
            ctx.disable();

            return Status.INVOICE_SUCCESSFULLY_PAID.args(Bot.fmtMoney(value));
        } catch (DataAccessException e) {
            chargeAction.rollback();
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }
}
