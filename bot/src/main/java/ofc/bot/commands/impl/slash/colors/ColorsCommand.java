package ofc.bot.commands.impl.slash.colors;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.database.repository.ColorRoleItemRepository;
import ofc.bot.domain.database.repository.ColorRoleStateRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.shop.ColorRoleRefundPolicy;
import ofc.bot.handlers.shop.ColorRoleStoreMessageFactory;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@DiscordCommand(name = "colors")
public class ColorsCommand extends SlashCommand {
    private final ColorRoleItemRepository colorItemRepo;
    private final ColorRoleStateRepository colorStateRepo;

    public ColorsCommand(ColorRoleItemRepository colorItemRepo, ColorRoleStateRepository colorStateRepo) {
        this.colorItemRepo = colorItemRepo;
        this.colorStateRepo = colorStateRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        User user = ctx.getUser();
        long guildId = guild.getIdLong();
        long userId = user.getIdLong();
        Map<Long, ColorRoleState> states = colorStateRepo.findByUserId(userId).stream()
                .filter(state -> state.getGuildId() == guildId)
                .collect(Collectors.toMap(ColorRoleState::getRoleId, Function.identity(), (first, ignored) -> first));

        List<ColorRoleStoreMessageFactory.Entry> entries = colorItemRepo.findAll()
                .stream()
                .map(color -> toEntry(guild, user, states, color))
                .filter(Objects::nonNull)
                .toList();

        if (entries.isEmpty()) {
            return Status.PAGE_IS_EMPTY;
        }

        Container store = ColorRoleStoreMessageFactory.create(entries);
        return ctx.create()
                .setUsingComponentsV2(true)
                .setComponents(store)
                .noMentions()
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra todos os cargos de cor disponiveis.";
    }

    private ColorRoleStoreMessageFactory.Entry toEntry(
            Guild guild,
            User user,
            Map<Long, ColorRoleState> states,
            ColorRoleItem color
    ) {
        Role role = guild.getRoleById(color.getRoleId());
        if (role == null) {
            return null;
        }

        ColorRoleState state = states.get(color.getRoleId());
        Button button = state == null
                ? EntityContextFactory.createColorRoleStorePurchaseButton(color, role, user)
                : EntityContextFactory.createColorRoleStoreRemoveButton(
                        state,
                        role,
                        user,
                        ColorRoleRefundPolicy.isRefundable(state)
                );

        return new ColorRoleStoreMessageFactory.Entry(color.getRoleId(), state, button);
    }
}
