package ofc.bot.listeners.discord.interactions.buttons.groups;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import ofc.bot.domain.entity.GroupBot;
import ofc.bot.domain.entity.OficinaGroup;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@InteractionHandler(scope = Scopes.Group.ADD_BOT, autoResponseType = AutoResponseType.THINKING)
public class GroupBotAddHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupBotAddHandler.class);
    private static final BetManager betManager = BetManager.getManager();

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int price = ctx.get("amount");
        GroupBot bot = ctx.get("bot");
        OficinaGroup group = ctx.get("group");
        Guild guild = ctx.getGuild();
        TextChannel textChan = group.getTextChannel();
        PaymentManager bank = PaymentManagerProvider.fromType(group.getCurrency());
        long ownerId = group.getOwnerId();
        long guildId = guild.getIdLong();

        if (betManager.isBetting(ownerId))
            return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;

        BankAction chargeAction = bank.charge(ownerId, guildId, 0, price, "Group bot added");
        if (!chargeAction.isOk()) {
            return Status.INSUFFICIENT_BALANCE;
        }

        if (textChan == null)
            return Status.YOUR_GROUP_DOES_NOT_HAVE_TEXT_CHANNEL;

        modifyChannelPermissions(textChan, bot.getBotId()).queue(v -> {
            ctx.reply(Status.GROUP_BOT_SUCCESSFULLY_ADDED.args(bot.getBotMention()));
        }, (err) -> {
            LOGGER.error("Could not add bot to group {}", group.getId(), err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        ctx.disable();
        return Status.OK;
    }

    private List<Permission> getChannelPermissions() {
        return Arrays.stream(Permission.values())
                .filter(Permission::isChannel)
                .toList();
    }

    private RestAction<?> modifyChannelPermissions(TextChannel channel, long botId) {
        return channel.getManager()
                .putMemberPermissionOverride(botId, getChannelPermissions(), null);
    }
}
