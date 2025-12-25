package ofc.bot.commands.impl.slash.colors;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@DiscordCommand(name = "color add")
public class AddColorRoleCommand extends SlashSubcommand {
    private final ColorRoleItemRepository colorItemRepo;

    public AddColorRoleCommand(ColorRoleItemRepository colorItemRepo) {
        this.colorItemRepo = colorItemRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String roleId = ctx.getSafeOption("color", OptionMapping::getAsString);
        Member member = ctx.getIssuer();
        User user = member.getUser();
        Guild guild = ctx.getGuild();
        ColorRoleItem color = colorItemRepo.findByRoleId(Long.parseLong(roleId));

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
                new OptionData(OptionType.STRING, "color", "A cor a ser adicionada", true, true)
        );
    }

    private boolean hasColorRole(Member member, Role role) {
        return member.getRoles().contains(role);
    }

    @DiscordEventHandler
    public static class ColorRoleListAutocompletionHandler extends ListenerAdapter {
        private final ColorRoleItemRepository colorItemRepo;

        public ColorRoleListAutocompletionHandler(ColorRoleItemRepository colorItemRepo) {
            this.colorItemRepo = colorItemRepo;
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
            String fullName = e.getFullCommandName();
            Guild guild = e.getGuild();
            User user = e.getUser();
            String focused = e.getFocusedOption().getName();

            if (!fullName.equals("color add") || !focused.equals("color")) return;

            List<ColorRoleItem> colors = colorItemRepo.findAll();
            List<Command.Choice> choices = toChoices(guild, colors);

            e.replyChoices(choices).queue();
        }

        private List<Command.Choice> toChoices(Guild guild, List<ColorRoleItem> colors) {
            return colors.stream()
                    .map(cri -> guild.getRoleById(cri.getRoleId()))
                    .filter(Objects::nonNull)
                    .map(r -> new Command.Choice(r.getName(), r.getId()))
                    .toList();
        }
    }
}