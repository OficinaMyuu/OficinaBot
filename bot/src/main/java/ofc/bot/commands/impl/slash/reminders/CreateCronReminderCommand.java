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
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "remind cron")
public class CreateCronReminderCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCronReminderCommand.class);
    private final ReminderRepository remRepo;

    public CreateCronReminderCommand(ReminderRepository remRepo) {
        this.remRepo = remRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User user = ctx.getUser();
        MessageChannel chan = ctx.getChannel();
        String expression = ctx.getSafeOption("expression", OptionMapping::getAsString);
        String message = ctx.getSafeOption("message", OptionMapping::getAsString);
        boolean isDm = ctx.getOption("privately", true, OptionMapping::getAsBoolean);
        long userId = ctx.getUserId();
        ChannelType chanType = isDm ? ChannelType.PRIVATE : chan.getType();
        long chanId = isDm ? userId : chan.getIdLong();

        int count = remRepo.countActiveByUserId(userId);
        if (count >= Reminder.MAX_PER_USER)
            return Status.HIT_MAX_REMINDERS;

        if (remRepo.existsByExpressionAndUserId(userId, expression))
            return Status.SAME_EXPRESSION_REMINDER_FOUND;

        if (!CronExpression.isValidExpression(expression))
            return Status.INVALID_CRON_EXPRESSION.args(expression);

        try {
            Reminder reminder = Reminder.ofCron(userId, chanId, chanType, message, expression);
            remRepo.save(reminder);

            MessageEmbed embed = EmbedFactory.embedCronReminder(user, expression);
            return ctx.replyEmbeds(embed);
        } catch (DataAccessException e) {
            LOGGER.error("Failed to save reminder to the database", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cria um lembrete baseado em um cron expression.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(10, TimeUnit.SECONDS);
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "expression", "A cron expression.", true),
                new OptionData(OptionType.STRING, "message", "A mensagem do lembrete.", true)
                        .setRequiredLength(2, 1000),
                new OptionData(OptionType.BOOLEAN, "privately", "Se devemos enviar o lembrete no privado (Padrão: True).")
        );
    }
}