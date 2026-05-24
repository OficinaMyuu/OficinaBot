package ofc.bot.commands.impl.slash.birthday;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.Birthday;
import ofc.bot.domain.entity.EntityPolicy;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.entity.enums.ResourceType;
import ofc.bot.domain.sqlite.repository.BirthdayRepository;
import ofc.bot.domain.sqlite.repository.EntityPolicyRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@DiscordCommand(name = "birthday add")
public class BirthdayAddCommand extends SlashSubcommand {
    private static final DateTimeFormatter END_USER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final Logger LOGGER = LoggerFactory.getLogger(BirthdayAddCommand.class);
    private final BirthdayRepository bdayRepo;
    private final EntityPolicyRepository policyRepo;

    public BirthdayAddCommand(BirthdayRepository bdayRepo, EntityPolicyRepository policyRepo) {
        this.bdayRepo = bdayRepo;
        this.policyRepo = policyRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String name = ctx.getSafeOption("name", OptionMapping::getAsString);
        String dateInput = ctx.getSafeOption("birthday", OptionMapping::getAsString);
        User user = ctx.getSafeOption("user", OptionMapping::getAsUser);
        int zoneHours = ctx.getSafeOption("time-zone", OptionMapping::getAsInt);
        boolean hideAge = ctx.getOption("hide-age", false, OptionMapping::getAsBoolean);
        long userId = user.getIdLong();

        try {
            LocalDate date = LocalDate.parse(dateInput, END_USER_DATE_FORMATTER);
            Birthday birthday = new Birthday(userId, name, date, zoneHours);

            bdayRepo.upsert(birthday);

            if (hideAge)
                addToExceptionList(userId);

            return Status.BIRTHDAY_ADDED_SUCCESSFULLY.args(user.getAsMention(), name);
        } catch (DateTimeParseException e) {
            return Status.INVALID_DATE_FORMAT;
        } catch (DataAccessException e) {
            LOGGER.error("Could not store birthday of '{}'", userId, e);
            return Status.COULD_NOT_ADD_BIRTHDAY;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Salva um aniversário na agenda.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        int minZone = ZoneOffset.MIN.getTotalSeconds() / 3600;
        int maxZone = ZoneOffset.MAX.getTotalSeconds() / 3600;

        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a ter o aniversário salvo.", true),
                new OptionData(OptionType.STRING, "name", "O nome que o usuário gosta de ser chamado.", true)
                        .setRequiredLength(2, 128),

                new OptionData(OptionType.STRING, "birthday", "A data de nascimento do usuário fornecido (Formato: DD/MM/AAAA).", true),
                new OptionData(OptionType.INTEGER, "time-zone", "O fuso horário local do membro.", true)
                        .setRequiredRange(minZone, maxZone),

                new OptionData(OptionType.BOOLEAN, "hide-age", "Deve-se ocultar a idade do usuário ao notificar o aniversário?")
        );
    }

    private void addToExceptionList(long userId) {
        EntityPolicy exclusion = new EntityPolicy(PolicyType.HIDE_BIRTHDAY_AGE, ResourceType.USER, userId);
        policyRepo.save(exclusion);
    }
}