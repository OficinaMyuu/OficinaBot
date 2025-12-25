package ofc.bot.commands.impl.slash.colors;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "color add")
public class AddColorRoleCommand extends SlashCommand {
    private final ColorRoleItemRepository colorItemRepo;

    public AddColorRoleCommand(ColorRoleItemRepository colorItemRepo) {
        this.colorItemRepo = colorItemRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        int colorId = ctx.getSafeOption("color", OptionMapping::getAsInt);
        Member member = ctx.getIssuer();
        User user = member.getUser();
        Guild guild = ctx.getGuild();
        ColorRoleItem color = colorItemRepo.findById(colorId);

        if (color == null)
            return Status.COLOR_ROLE_NOT_FOUND;

        Role role = guild.getRoleById(color.getRoleId());
        if (role == null)
            return Status.ROLE_NOT_FOUND;

        if (hasColorRole(member, role))
            return Status.YOU_ALREADY_HAVE_THIS_COLOR_ROLE;

        MessageEmbed embed = EmbedFactory.embedColorRolePurchase(color, role, user);
        List<Button> confirms = EntityContextFactory.createColorRoleButtons(color, role, user);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(confirms)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Adiciona um cargo de cor à você";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "color", "A cor a ser adicionada", true, true)
        );
    }

    private boolean hasColorRole(Member member, Role role) {
        return member.getRoles().contains(role);
    }
}