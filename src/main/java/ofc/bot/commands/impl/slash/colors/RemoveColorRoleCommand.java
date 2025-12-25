package ofc.bot.commands.impl.slash.colors;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "color remove")
public class RemoveColorRoleCommand extends SlashCommand {
    private static final int REFUND_PERIOD_MILLIS = 5 * 60 * 1000; // 5 minutes
    private final ColorRoleStateRepository colorStateRepo;
    private final ColorRoleItemRepository colorItemRepo;

    public RemoveColorRoleCommand(ColorRoleStateRepository colorStateRepo, ColorRoleItemRepository colorItemRepo) {
        this.colorStateRepo = colorStateRepo;
        this.colorItemRepo = colorItemRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        int colorId = ctx.getSafeOption("color", OptionMapping::getAsInt);
        Member member = ctx.getIssuer();
        User user = ctx.getUser();
        Guild guild = ctx.getGuild();
        ColorRoleItem color = colorItemRepo.findById(colorId);
        long userId = user.getIdLong();

        if (color == null)
            return Status.COLOR_ROLE_NOT_FOUND;

        Role role = guild.getRoleById(color.getRoleId());
        if (role == null)
            return Status.ROLE_NOT_FOUND;

        ColorRoleState state = colorStateRepo.findByUserAndRoleId(userId, role.getIdLong());
        if (state == null)
            return Status.YOU_DO_NOT_HAVE_THIS_COLOR_ROLE;

        boolean shouldRefund = shouldRefund(state);
        MessageEmbed embed = EmbedFactory.embedColorRoleRemotion(user, state, role, shouldRefund);
        List<Button> confirm = EntityContextFactory.createRemoveColorRoleButtons(state, role, user, shouldRefund);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(confirm)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Remove um cargo de cor de você";
    }

    private boolean shouldRefund(ColorRoleState state) {
        long now = Bot.unixNow();
        return state != null && now - state.getTimeCreated() < REFUND_PERIOD_MILLIS;
    }
}