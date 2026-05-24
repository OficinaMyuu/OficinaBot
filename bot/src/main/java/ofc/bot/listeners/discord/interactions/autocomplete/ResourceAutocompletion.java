package ofc.bot.listeners.discord.interactions.autocomplete;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.enums.ResourceType;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;
import java.util.function.Function;

@DiscordEventHandler
public class ResourceAutocompletion extends ListenerAdapter {
    private final UserRepository userRepo;

    public ResourceAutocompletion(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        AutoCompleteQuery focus = e.getFocusedOption();
        String name = focus.getName();
        Guild guild = e.getGuild();

        if (!"resource".equals(name) || focus.getType() != OptionType.STRING) return;
        if (guild == null) return;

        String value = focus.getValue().toLowerCase();
        String resTypeName = e.getOption("resource-type", OptionMapping::getAsString);
        ResourceType resType = ResourceType.valueOf(resTypeName);
        List<Command.Choice> choices = getChoices(resType, guild, value);

        e.replyChoices(choices).queue();
    }

    private List<Command.Choice> getChoices(ResourceType resType, Guild guild, String value) {
        return switch (resType) {
            case USER -> {
                List<AppUser> users = userRepo.findByNames(value, OptionData.MAX_CHOICES);
                yield intoChoices(users, (u) -> new Command.Choice(u.getDisplayName(), u.getId()));
            }
            case ROLE -> {
                List<Role> roles = getFiltered(guild.getRoles(), value, Role::getName);
                yield intoChoices(roles, (r) -> new Command.Choice(r.getName(), r.getId()));
            }
            case CHANNEL -> {
                List<GuildChannel> chans = getFiltered(guild.getChannels(), value, GuildChannel::getName);
                yield intoChoices(chans, (c) -> new Command.Choice(c.getName(), c.getId()));
            }
            // Here, the `value` parameter is the domain itself, so all we can do is return the value lol
            case LINK -> List.of(new Command.Choice(value, value));
        };
    }

    private <T> List<T> getFiltered(List<T> list, String search, Function<T, String> field) {
        return list.stream()
                .filter(el -> field.apply(el).toLowerCase().contains(search))
                .limit(OptionData.MAX_CHOICES)
                .toList();
    }

    private <T> List<Command.Choice> intoChoices(List<T> list, Function<T, Command.Choice> mapper) {
        return list.stream()
                .map(mapper)
                .toList();
    }
}