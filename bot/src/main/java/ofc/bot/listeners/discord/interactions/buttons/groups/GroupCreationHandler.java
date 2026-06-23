package ofc.bot.listeners.discord.interactions.buttons.groups;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.handlers.economy.BankAction;
import ofc.bot.handlers.economy.PaymentManager;
import ofc.bot.handlers.economy.PaymentManagerProvider;
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

@InteractionHandler(scope = Scopes.Group.CREATE_GROUP, autoResponseType = AutoResponseType.DEFER_EDIT)
public class GroupCreationHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCreationHandler.class);
    private static final BetManager betManager = BetManager.getManager();
    private final OficinaGroupRepository grpRepo;

    public GroupCreationHandler(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    public InteractionResult onExecute(ButtonClickContext ctx) {
        OficinaGroup group = ctx.get("group");
        PaymentManager bank = PaymentManagerProvider.fromType(group.getCurrency());
        Guild guild = ctx.getGuild();
        long guildId = guild.getIdLong();
        long ownerId = group.getOwnerId();
        int price = group.getAmountPaid();

        if (betManager.isBetting(ownerId))
            return edit(ctx, Status.YOU_CANNOT_DO_THIS_WHILE_BETTING);

        if (grpRepo.existsByEmoji(group.getEmoji()))
            return edit(ctx, Status.GROUP_EMOJI_ALREADY_IN_USE);

        ctx.create()
                .setContent(Status.CREATING_GROUP)
                .setComponents()
                .edit();

        BankAction chargeAction = bank.charge(ownerId, guildId, 0, price, "Group created");
        if (!chargeAction.isOk()) {
            return edit(ctx, Status.INSUFFICIENT_BALANCE);
        }

        try {
            int color = ctx.get("group_color");
            Role groupRole = createRole(guild, group, color);
            long roleId = groupRole.getIdLong();
            long timestamp = Bot.unixNow();

            // :>
            addRoleToMembers(guild, groupRole, ownerId);

            group.setRoleId(roleId)
                    .setTimeCreated(timestamp)
                    .setLastUpdated(timestamp);
            grpRepo.upsert(group);

            return edit(ctx, Status.GROUP_SUCCESSFULLY_CREATED.args(groupRole.getAsMention()));
        } catch (ErrorResponseException e) {
            LOGGER.error("Could not create group for member with id {}", ownerId, e);

            chargeAction.rollback();
            return edit(ctx, Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        }
    }

    private void addRoleToMembers(Guild guild, Role role, long ownerId) {
        guild.addRoleToMember(UserSnowflake.fromId(742729586659295283L), role).queue();
        guild.addRoleToMember(UserSnowflake.fromId(ownerId), role).queue();
    }

    private Role createRole(Guild guild, OficinaGroup group, int color) {
        String roleName = group.getRoleName();
        Role role = guild.createRole()
                .setName(roleName)
                .setColor(color)
                .setPermissions(0L)
                .complete();

        adjustRolePosition(role);
        return role;
    }

    private InteractionResult edit(ButtonClickContext ctx, InteractionResult result) {
        return ctx.create()
                .setContent(result)
                .setComponents()
                .edit(result);
    }

    private void adjustRolePosition(Role role) {
        Guild guild = role.getGuild();
        Role anchor = guild.getRoleById(OficinaGroup.ANCHOR_GROUP_ROLE_ID);

        if (anchor == null) return;

        guild.modifyRolePositions()
                .selectPosition(role)
                .moveAbove(anchor)
                .complete();
    }
}
