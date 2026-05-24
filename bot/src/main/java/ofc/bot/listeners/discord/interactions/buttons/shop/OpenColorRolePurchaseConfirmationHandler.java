package ofc.bot.listeners.discord.interactions.buttons.shop;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Shop.OPEN_COLOR_ROLE_PURCHASE_CONFIRMATION, autoResponseType = AutoResponseType.THINKING)
public class OpenColorRolePurchaseConfirmationHandler implements InteractionListener<ButtonClickContext> {
    private final ColorRoleItemRepository colorItemRepo;
    private final ColorRoleStateRepository colorStateRepo;

    public OpenColorRolePurchaseConfirmationHandler(
            ColorRoleItemRepository colorItemRepo,
            ColorRoleStateRepository colorStateRepo
    ) {
        this.colorItemRepo = colorItemRepo;
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

        ColorRoleItem color = colorItemRepo.findByRoleId(role.getIdLong());
        if (color == null) {
            return Status.COLOR_ROLE_NOT_FOUND;
        }

        if (ctx.getIssuer().getRoles().contains(role)
                || colorStateRepo.findByUserAndRoleId(user.getIdLong(), role.getIdLong()) != null) {
            return Status.YOU_ALREADY_HAVE_THIS_COLOR_ROLE;
        }

        MessageEmbed embed = EmbedFactory.embedColorRolePurchase(color, role, user);
        List<Button> buttons = EntityContextFactory.createColorRoleButtons(color, role, user);
        ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
        ctx.disable();
        return Status.OK;
    }
}
