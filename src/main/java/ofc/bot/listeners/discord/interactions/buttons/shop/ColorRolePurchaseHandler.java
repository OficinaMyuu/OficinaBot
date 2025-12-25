package ofc.bot.listeners.discord.interactions.buttons.shop;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.handlers.economy.*;
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

@InteractionHandler(scope = Scopes.Shop.ADD_COLOR_ROLE, autoResponseType = AutoResponseType.DEFER_EDIT)
public class ColorRolePurchaseHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorRolePurchaseHandler.class);
    private final ColorRoleStateRepository colorStateRepo;

    public ColorRolePurchaseHandler(ColorRoleStateRepository colorStateRepo) {
        this.colorStateRepo = colorStateRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        User user = ctx.get("user");
        Role role = ctx.get("role");
        ColorRoleItem color = ctx.get("color");
        CurrencyType currency = ctx.get("currency");
        PaymentManager payment = PaymentManagerProvider.fromType(currency);
        long userId = user.getIdLong();
        long guildId = guild.getIdLong();
        long now = Bot.unixNow();
        BankAction act = payment.charge(userId, guildId, 0, color.getPrice(), "Color role purchase");

        if (!act.isOk())
            return Status.INSUFFICIENT_BALANCE;

        guild.addRoleToMember(user, role).queue(s -> {
            ColorRoleState state = new ColorRoleState(userId, guildId, role.getIdLong(), now, now);

            colorStateRepo.save(state);
            ctx.reply(Status.COLOR_ROLE_SUCCESSFULLY_ADDED.args(role.getAsMention()));
        }, (err) -> {
            LOGGER.error("Failed to give color role {} to user {}", role.getId(), userId, err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);

            // Refund the user if we are not able to give the color role
            act.rollback();
        });
        return Status.OK;
    }
}