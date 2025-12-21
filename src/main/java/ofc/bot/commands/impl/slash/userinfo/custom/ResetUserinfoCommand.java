package ofc.bot.commands.impl.slash.userinfo.custom;

import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.sqlite.repository.CustomUserinfoRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordCommand(name = "customize reset")
public class ResetUserinfoCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResetUserinfoCommand.class);
    private final CustomUserinfoRepository csinfoRepo;

    public ResetUserinfoCommand(CustomUserinfoRepository csinfoRepo) {
        this.csinfoRepo = csinfoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User sender = ctx.getUser();
        long userId = sender.getIdLong();

        try {
            csinfoRepo.deleteByUserId(userId);

            return Status.USERINFO_RESET_SUCCESSFULLY;
        } catch (DataAccessException e) {
            LOGGER.error("Could not reset userinfo customization of user '{}'", userId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Redefine todos os dados customizados do userinfo.";
    }
}