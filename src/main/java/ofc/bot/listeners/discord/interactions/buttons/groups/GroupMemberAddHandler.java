package ofc.bot.listeners.discord.interactions.buttons.groups;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import ofc.bot.domain.entity.GroupPerk;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.sqlite.repository.GroupPerkRepository;
import ofc.bot.handlers.economy.*;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InteractionHandler(scope = Scopes.Group.ADD_MEMBER, autoResponseType = AutoResponseType.THINKING)
public class GroupMemberAddHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupMemberAddHandler.class);
    private static final BetManager betManager = BetManager.getManager();
    private final GroupPerkRepository perksRepo;

    public GroupMemberAddHandler(GroupPerkRepository perksRepo) {
        this.perksRepo = perksRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        OficinaGroup group = ctx.get("group");
        Member newMember = ctx.get("new_member");
        int price = ctx.get("amount");
        long roleId = group.getRoleId();
        long ownerId = group.getOwnerId();
        long guildId = guild.getIdLong();
        CurrencyType currency = group.getCurrency();
        PaymentManager bank = PaymentManagerProvider.fromType(currency);
        Role groupRole = guild.getRoleById(roleId);

        if (betManager.isBetting(ownerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        if (groupRole == null)
            return Status.GROUP_ROLE_NOT_FOUND;

        BankAction chargeAction = bank.charge(ownerId, guildId, 0, price, "Group member added");
        if (!chargeAction.isOk()) {
            return Status.INSUFFICIENT_BALANCE;
        }

        guild.addRoleToMember(newMember, groupRole).queue(v -> {
            ctx.reply(Status.MEMBER_SUCCESSFULLY_ADDED_TO_GROUP.args(newMember.getAsMention()));
            regiserPurchase(group.getId(), ownerId, price, currency);
        }, (err) -> {
            LOGGER.error("Could not add role &{} to member @{}", roleId, newMember.getId());

            chargeAction.rollback();
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        ctx.disable();
        return Status.OK;
    }

    private void regiserPurchase(int groupId, long userId, int paid, CurrencyType currency) {
        GroupPerk perk = new GroupPerk(groupId, userId, StoreItemType.GROUP_SLOT, paid, currency, Bot.unixNow());
        perksRepo.save(perk);
    }
}