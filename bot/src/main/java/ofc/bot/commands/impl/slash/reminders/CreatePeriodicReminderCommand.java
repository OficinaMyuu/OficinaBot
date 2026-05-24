package ofc.bot.commands.impl.slash.reminders;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.Reminder;
import ofc.bot.domain.sqlite.repository.ReminderRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import ofc.bot.util.time.OficinaDuration;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "remind periodic")
public class CreatePeriodicReminderCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePeriodicReminderCommand.class);
    private final ReminderRepository remRepo;

    public CreatePeriodicReminderCommand(ReminderRepository remRepo) {
        this.remRepo = remRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User user = ctx.getUser();
        String fmtPeriod = ctx.getSafeOption("period", OptionMapping::getAsString);
        String message = ctx.getSafeOption("message", OptionMapping::getAsString);
        boolean isDm = ctx.getOption("privately", true, OptionMapping::getAsBoolean);
        int repeat = ctx.getOption("repeat", 0, OptionMapping::getAsInt);
        long userId = user.getIdLong();
        MessageChannel chan = ctx.getChannel();
        ChannelType chanType = isDm ? ChannelType.PRIVATE : chan.getType();
        long chanId = isDm ? userId : chan.getIdLong();

        int count = remRepo.countActiveByUserId(userId);
        if (count >= Reminder.MAX_PER_USER)
             return Status.HIT_MAX_REMINDERS.args(count);

        try {
            OficinaDuration duration = OficinaDuration.ofPattern(fmtPeriod);
            long secs = duration.getSeconds();

            if (secs < Reminder.MIN_DURATION)
                return Status.PERIOD_TOO_SHORT;

            Reminder reminder = Reminder.ofPeriod(userId, chanId, chanType, message, secs, repeat + 1);
            remRepo.save(reminder);

            MessageEmbed embed = EmbedFactory.embedPeriodicReminder(user, secs, repeat);
            return ctx.replyEmbeds(embed);
        } catch (IllegalArgumentException e) {
            return Status.INVALID_PERIOD_PROVIDED;
        } catch (DataAccessException e) {
            LOGGER.error("Failed to save reminder to the database", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cria um lembrete para executar periodicamente.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "period", "O período entre lembretes (Exemplo: 40m), mínimo: 2m.", true)
                        .setRequiredLength(2, 20),
                new OptionData(OptionType.STRING, "message", "A mensagem do lembrete.", true)
                        .setRequiredLength(2, 1000),
                new OptionData(OptionType.INTEGER, "repeat", "Quantas vezes devemos repetir este intervalo.")
                        .setRequiredRange(1, 2000),
                new OptionData(OptionType.BOOLEAN, "privately", "Se devemos enviar o lembrete no privado (Padrão: True).")
        );
    }
}