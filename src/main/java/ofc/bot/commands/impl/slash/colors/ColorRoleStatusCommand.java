package ofc.bot.commands.impl.slash.colors;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "color status")
public class ColorRoleStatusCommand extends SlashSubcommand {
    private final ColorRoleStateRepository colorStateRepo;

    public ColorRoleStatusCommand(ColorRoleStateRepository colorStateRepo) {
        this.colorStateRepo = colorStateRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        User user = ctx.getUser();
        Guild guild = ctx.getGuild();
        List<ColorRoleState> states = colorStateRepo.findByUserId(userId);

        if (states.isEmpty())
            return Status.NO_COLOR_ROLES_STATE;

        MessageEmbed embed = EmbedFactory.embedColorRoleStates(guild, user, states);
        return ctx.replyEmbeds(embed);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra o status de todos os seus cargos de cor.";
    }
}