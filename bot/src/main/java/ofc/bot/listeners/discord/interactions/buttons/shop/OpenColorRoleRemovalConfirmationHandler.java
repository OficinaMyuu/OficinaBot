package ofc.bot.listeners.discord.interactions.buttons.shop;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.database.repository.ColorRoleStateRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.shop.ColorRoleRefundPolicy;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Shop.OPEN_COLOR_ROLE_REMOVAL_CONFIRMATION, autoResponseType = AutoResponseType.THINKING)
public class OpenColorRoleRemovalConfirmationHandler implements InteractionListener<ButtonClickContext> {
    private final ColorRoleStateRepository colorStateRepo;

    public OpenColorRoleRemovalConfirmationHandler(ColorRoleStateRepository colorStateRepo) {
        this.colorStateRepo = colorStateRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        User user = ctx.get("user");
        Role storedRole = ctx.get("role");
        Role role = guild.getRoleById(storedRole.getIdLong());
        if (role == null) {
            return Status.ROLE_NOT_FOUND;
        }

        ColorRoleState state = colorStateRepo.findByUserAndRoleId(user.getIdLong(), role.getIdLong());
        if (state == null) {
            return Status.YOU_DO_NOT_HAVE_THIS_COLOR_ROLE;
        }

        boolean shouldRefund = ColorRoleRefundPolicy.isRefundable(state);
        MessageEmbed embed = EmbedFactory.embedColorRoleRemotion(user, state, role, shouldRefund);
        List<Button> buttons = EntityContextFactory.createRemoveColorRoleButtons(state, role, user, shouldRefund);
        ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
        ctx.disable();
        return Status.OK;
    }
}
