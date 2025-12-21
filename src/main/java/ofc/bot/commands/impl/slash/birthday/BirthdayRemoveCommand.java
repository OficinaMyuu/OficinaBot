package ofc.bot.commands.impl.slash.birthday;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.Birthday;
import ofc.bot.domain.sqlite.repository.BirthdayRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "birthday remove")
public class BirthdayRemoveCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BirthdayRemoveCommand.class);
    private final BirthdayRepository bdayRepo;

    public BirthdayRemoveCommand(BirthdayRepository bdayRepo) {
        this.bdayRepo = bdayRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        long targetId = target.getIdLong();
        Birthday birthday = bdayRepo.findByUserId(targetId);

        if (birthday == null) return Status.USER_IS_NOT_IN_BIRTHDAY_LIST;

        try {
            bdayRepo.delete(birthday);

            return Status.BIRTHDAY_DELETED_SUCCESSFULLY.args(targetId);
        } catch (DataAccessException e) {
            LOGGER.error("Could not remove user '{}' from birthday list", targetId, e);
            return Status.COULD_NOT_REMOVE_BIRTHDAY;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Remove um aniversário da agenda.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a ser removido.", true)
        );
    }
}