package ofc.bot.commands.slash.birthday;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.Birthday;
import ofc.bot.domain.sqlite.repository.BirthdayRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@DiscordCommand(name = "birthdays", permissions = Permission.MANAGE_SERVER)
public class BirthdaysCommand extends SlashCommand {
    private final BirthdayRepository bdayRepo;

    public BirthdaysCommand(BirthdayRepository bdayRepo) {
        this.bdayRepo = bdayRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Month month = ctx.getEnumOption("month", LocalDate.now().getMonth(), Month.class);
        Guild guild = ctx.getGuild();
        List<Birthday> birthdays = bdayRepo.findByMonth(month);
        MessageEmbed embed = EmbedFactory.embedBirthdayList(birthdays, guild, month);
        List<Button> buttons = EntityContextFactory.createBirthdayListButtons(month);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Veja o aniversário de todos os membros da staff.";
    }

    @Override
    public @NotNull List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "month", "O mês dos aniversariantes.")
                        .addChoices(getMonthChoices())
        );
    }

    private List<Command.Choice> getMonthChoices() {
        return Stream.of(Month.values())
                .map(m -> new Command.Choice(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH), m.name()))
                .toList();
    }
}
