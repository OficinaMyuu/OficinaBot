package ofc.bot.listeners.discord.interactions.buttons.groups;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
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
import ofc.bot.util.GroupHelper;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InteractionHandler(scope = Scopes.Group.UPDATE_GROUP, autoResponseType = AutoResponseType.THINKING)
public class GroupUpdateHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupUpdateHandler.class);
    private static final BetManager betManager = BetManager.getManager();
    private final OficinaGroupRepository grpRepo;

    public GroupUpdateHandler(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        OficinaGroup group = ctx.get("group");
        String newName = ctx.find("new_name");
        Guild guild = ctx.getGuild();
        PaymentManager bank = PaymentManagerProvider.fromType(group.getCurrency());
        long ownerId = group.getOwnerId();
        long guildId = guild.getIdLong();
        int newColor = ctx.get("new_color");
        int price = ctx.get("amount");
        boolean changedName = newName != null && !newName.isBlank();

        if (betManager.isBetting(ownerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        if (changedName)
            group.setName(newName);

        BankAction chargeAction = bank.charge(ownerId, guildId, 0, price, "Group data updated");
        if (!chargeAction.isOk()) {
            return Status.INSUFFICIENT_BALANCE;
        }

        try {
            modifyRole(guild, group, newColor);

            if (changedName) {
                modifyChannels(group);
            }

            grpRepo.upsert(group.tickUpdate());

            ctx.disable();
            return Status.GROUP_SUCCESSFULLY_UPDATED;
        } catch (DataAccessException | ErrorResponseException e) {
            LOGGER.error("Failed to update group for ID {}", group.getId(), e);
            chargeAction.rollback();
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    private void modifyRole(Guild guild, OficinaGroup group, int newColor) {
        long roleId = group.getRoleId();
        Role role = guild.getRoleById(roleId);
        String roleName = String.format(OficinaGroup.ROLE_NAME_FORMAT, group.getName());

        if (role == null) return;

        role.getManager()
                .setName(roleName)
                .setColor(newColor == -1 ? role.getColorRaw() : newColor)
                .complete();
    }

    private void modifyChannels(OficinaGroup group) {
        TextChannel textChan = group.getTextChannel();
        VoiceChannel voiceChan = group.getVoiceChannel();

        if (textChan != null)
            textChan.getManager()
                    .setName(group.getTextChannelName())
                    .complete();

        if (voiceChan != null)
            voiceChan.getManager()
                    .setName(group.getVoiceChannelName())
                    .complete();
    }
}
