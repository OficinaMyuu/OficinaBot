package ofc.bot.commands.impl.slash.colors;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "color list")
public class ListColorsRolesCommand extends SlashCommand {
    private final ColorRoleItemRepository colorItemRepo;

    public ListColorsRolesCommand(ColorRoleItemRepository colorItemRepo) {
        this.colorItemRepo = colorItemRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        List<ColorRoleItem> roles = colorItemRepo.findAll();

        if (roles.isEmpty())
            return Status.PAGE_IS_EMPTY;

        MessageEmbed embed = EmbedFactory.embedColorRolesList(guild, roles);
        return ctx.replyEmbeds(embed);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra todos os cargos de cor disponíveis";
    }
}