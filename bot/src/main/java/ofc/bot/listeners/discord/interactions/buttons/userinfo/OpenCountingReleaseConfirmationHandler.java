package ofc.bot.listeners.discord.interactions.buttons.userinfo;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.userinfo.CountingReleaseService;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;
import java.util.OptionalLong;

@InteractionHandler(scope = Scopes.Userinfo.OPEN_COUNTING_RELEASE_CONFIRMATION, autoResponseType = AutoResponseType.THINKING)
public class OpenCountingReleaseConfirmationHandler implements InteractionListener<ButtonClickContext> {
    private final CountingReleaseService service;

    public OpenCountingReleaseConfirmationHandler() {
        this(new CountingReleaseService());
    }

    OpenCountingReleaseConfirmationHandler(CountingReleaseService service) {
        this.service = service;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        User user = ctx.get("user");
        OptionalLong roleId = service.findPunishmentRoleId();

        if (roleId.isEmpty()) {
            return Status.ROLE_NOT_FOUND;
        }

        Role role = guild.getRoleById(roleId.getAsLong());
        if (role == null) {
            return Status.ROLE_NOT_FOUND;
        }

        MessageEmbed embed = EmbedFactory.embedCountingReleasePurchase(guild, user, role);
        List<Button> buttons = EntityContextFactory.createCountingReleasePaymentButtons(user);

        ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
        ctx.disable();
        return Status.OK;
    }
}
