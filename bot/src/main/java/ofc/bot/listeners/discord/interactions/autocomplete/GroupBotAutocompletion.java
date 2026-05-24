package ofc.bot.listeners.discord.interactions.autocomplete;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.GroupBot;
import ofc.bot.domain.sqlite.repository.GroupBotRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;

@DiscordEventHandler
public class GroupBotAutocompletion extends ListenerAdapter {
    private final GroupBotRepository grpBotRepo;

    public GroupBotAutocompletion(GroupBotRepository grpBotRepo) {
        this.grpBotRepo = grpBotRepo;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        AutoCompleteQuery focused = e.getFocusedOption();
        String name = focused.getName();

        if (!name.equals("bot") || focused.getType() != OptionType.INTEGER) return;

        List<GroupBot> bots = grpBotRepo.findByName(focused.getValue(), OptionData.MAX_CHOICES);
        List<Command.Choice> choices = mapToChoices(bots);

        e.replyChoices(choices).queue();
    }

    private List<Command.Choice> mapToChoices(List<GroupBot> bots) {
        return bots.stream()
                .map(bot -> new Command.Choice(bot.getBotName(), bot.getId()))
                .toList();
    }
}
