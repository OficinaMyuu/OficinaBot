package ofc.bot.commands.impl.slash.colors;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@DiscordCommand(name = "color remove")
public class RemoveColorRoleCommand extends SlashSubcommand {
    private static final int REFUND_PERIOD_MILLIS = 5 * 60 * 1000; // 5 minutes
    private final ColorRoleStateRepository colorStateRepo;
    private final ColorRoleItemRepository colorItemRepo;

    public RemoveColorRoleCommand(ColorRoleStateRepository colorStateRepo, ColorRoleItemRepository colorItemRepo) {
        this.colorStateRepo = colorStateRepo;
        this.colorItemRepo = colorItemRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String roleId = ctx.getSafeOption("color", OptionMapping::getAsString);
        User user = ctx.getUser();
        Guild guild = ctx.getGuild();
        ColorRoleItem color = colorItemRepo.findByRoleId(Long.parseLong(roleId));
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

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "color", "A cor a ser adicionada", true, true)
        );
    }

    private boolean shouldRefund(ColorRoleState state) {
        long now = Bot.unixNow();
        return state != null && now - state.getTimeCreated() < (REFUND_PERIOD_MILLIS / 1000);
    }

    @DiscordEventHandler
    public static class ColorRoleAutocompletionHandler extends ListenerAdapter {
        private final ColorRoleStateRepository colorStateRepo;

        public ColorRoleAutocompletionHandler(ColorRoleStateRepository colorStateRepo) {
            this.colorStateRepo = colorStateRepo;
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
            String fullName = e.getFullCommandName();
            Guild guild = e.getGuild();
            User user = e.getUser();
            AutoCompleteQuery focused = e.getFocusedOption();
            String focusName = focused.getName();
            long userId = user.getIdLong();

            if (!fullName.equals("color remove") || !focusName.equals("color")) return;

            String search = focused.getValue().strip().toLowerCase();
            List<ColorRoleState> roles = colorStateRepo.findByUserId(userId);
            List<Command.Choice> choices = toChoices(search, guild, roles);

            e.replyChoices(choices).queue();
        }

        private List<Command.Choice> toChoices(String search, Guild guild, List<ColorRoleState> roles) {
            return roles.stream()
                    .map(crs -> guild.getRoleById(crs.getRoleId()))
                    .filter(Objects::nonNull)
                    .filter(r -> r.getName().toLowerCase().contains(search))
                    .map(r -> new Command.Choice(r.getName(), r.getId()))
                    .toList();
        }
    }
}