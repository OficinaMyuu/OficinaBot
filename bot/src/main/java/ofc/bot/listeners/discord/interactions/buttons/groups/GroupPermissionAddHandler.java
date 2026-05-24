package ofc.bot.listeners.discord.interactions.buttons.groups;

import net.dv8tion.jda.api.entities.Guild;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.GroupPermission;
import ofc.bot.domain.sqlite.repository.EntityPolicyRepository;
import ofc.bot.handlers.cache.PolicyService;
import ofc.bot.handlers.economy.*;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.groups.permissions.GroupPermissionManager;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;

@InteractionHandler(scope = Scopes.Group.ADD_PERMISSION, autoResponseType = AutoResponseType.THINKING)
public class GroupPermissionAddHandler implements InteractionListener<ButtonClickContext> {
    private static final BetManager betManager = BetManager.getManager();
    private final PolicyService policyService = PolicyService.getService();
    private final GroupPermissionManager permissionManager;

    public GroupPermissionAddHandler(EntityPolicyRepository policyRepo) {
        this.permissionManager = new GroupPermissionManager(policyRepo);
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int price = ctx.get("amount");
        OficinaGroup group = ctx.get("group");
        GroupPermission perm = ctx.get("permission");
        CurrencyType currency = group.getCurrency();
        PaymentManager bank = PaymentManagerProvider.fromType(currency);
        Guild guild = ctx.getGuild();
        long ownerId = group.getOwnerId();
        long guildId = guild.getIdLong();

        if (betManager.isBetting(ownerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        BankAction chargeAction = bank.charge(ownerId, guildId, 0, price, "Group Permission Added");
        if (!chargeAction.isOk()) {
            return Status.INSUFFICIENT_BALANCE;
        }

        boolean success = permissionManager.grant(perm, group);
        if (!success) {
            chargeAction.rollback();
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }

        if (!perm.isDiscord()) {
            // Invalidate all the cache to keep it up-to-date with the new policies set.
            policyService.invalidate();
        }

        ctx.disable();
        return Status.GROUP_PERMISSION_GRANTED_SUCESSFULLY.args(perm.getDisplay());
    }
}