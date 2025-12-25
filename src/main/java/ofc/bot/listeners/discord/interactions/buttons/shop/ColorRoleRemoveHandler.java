package ofc.bot.listeners.discord.interactions.buttons.shop;

import net.dv8tion.jda.api.entities.*;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.handlers.economy.*;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InteractionHandler(scope = Scopes.Shop.REMOVE_COLOR_ROLE, autoResponseType = AutoResponseType.DEFER_EDIT)
public class ColorRoleRemoveHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorRolePurchaseHandler.class);
    private final ColorRoleStateRepository colorStateRepo;

    public ColorRoleRemoveHandler(ColorRoleStateRepository colorStateRepo) {
        this.colorStateRepo = colorStateRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int refund = ctx.get("refund");
        User user = ctx.get("user");
        Role role = ctx.get("role");
        Guild guild = role.getGuild();
        CurrencyType currency = ctx.get("currency");
        PaymentManager bank = PaymentManagerProvider.fromType(currency);
        boolean hasRefund = refund > 0;
        long guildId = guild.getIdLong();
        long userId = user.getIdLong();
        long roleId = role.getIdLong();

        guild.removeRoleFromMember(user, role).queue(s -> {
            colorStateRepo.deleteByUserAndRoleId(userId, roleId);

            if (hasRefund) {
                bank.update(userId, guildId, 0, refund, "Reembolso de cargo de cor");
            }

            MessageEmbed embed = EmbedFactory.embedColorRoleRemoved(user, role);
            ctx.replyEmbeds(embed);
            ctx.disableAll();
        }, (err) -> {
            LOGGER.error("Failed to remove color role {} from user {}", roleId, userId, err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });

        return Status.OK;
    }
}