package ofc.bot.listeners.discord.interactions.autocomplete;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;

@DiscordEventHandler
public class OficinaGroupAutocompletion extends ListenerAdapter {
    private final OficinaGroupRepository grpRepo;

    public OficinaGroupAutocompletion(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        AutoCompleteQuery focused = e.getFocusedOption();
        String name = focused.getName();

        if (!name.equals("group") || focused.getType() != OptionType.INTEGER) return;

        List<OficinaGroup> groups = grpRepo.findByName(focused.getValue(), OptionData.MAX_CHOICES);
        List<Command.Choice> choices = mapToChoices(groups);

        e.replyChoices(choices).queue();
    }

    private List<Command.Choice> mapToChoices(List<OficinaGroup> groups) {
        return groups.stream()
                .map(grp -> new Command.Choice(grp.getName(), grp.getId()))
                .toList();
    }
}