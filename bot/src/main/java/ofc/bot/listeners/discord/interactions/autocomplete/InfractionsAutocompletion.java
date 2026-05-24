package ofc.bot.listeners.discord.interactions.autocomplete;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.MemberPunishment;
import ofc.bot.domain.sqlite.repository.MemberPunishmentRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;

@DiscordEventHandler
public class InfractionsAutocompletion extends ListenerAdapter {
    private final MemberPunishmentRepository pnshRepo;

    public InfractionsAutocompletion(MemberPunishmentRepository pnshRepo) {
        this.pnshRepo = pnshRepo;
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        AutoCompleteQuery focused = e.getFocusedOption();
        String name = focused.getName();

        if (!name.equals("infraction") || focused.getType() != OptionType.INTEGER) return;

        Member target = e.getOption("member", OptionMapping::getAsMember);
        if (target == null) {
            e.replyChoice("Por favor, selecione um membro.", -1).queue();
            return;
        }

        Guild guild = target.getGuild();
        long targetId = target.getIdLong();
        long guildId = guild.getIdLong();

        List<MemberPunishment> punishments = pnshRepo.findByUserAndGuildId(targetId, guildId, OptionData.MAX_CHOICES);
        List<Command.Choice> choices = mapToChoices(punishments);

        e.replyChoices(choices).queue();
    }

    private List<Command.Choice> mapToChoices(List<MemberPunishment> punishments) {
        return punishments.stream()
                .map(p -> new Command.Choice(Bot.limitStr(p.getReason(), Command.Choice.MAX_NAME_LENGTH), p.getId()))
                .toList();
    }
}
