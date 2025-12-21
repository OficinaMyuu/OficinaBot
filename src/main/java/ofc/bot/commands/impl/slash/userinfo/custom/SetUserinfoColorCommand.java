package ofc.bot.commands.impl.slash.userinfo.custom;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.CustomUserinfo;
import ofc.bot.domain.sqlite.repository.CustomUserinfoRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "customize color")
public class SetUserinfoColorCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetUserinfoColorCommand.class);
    private final CustomUserinfoRepository csinfoRepo;

    public SetUserinfoColorCommand(CustomUserinfoRepository csinfoRepo) {
        this.csinfoRepo = csinfoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String input = ctx.getOption("color", null, OptionMapping::getAsString);
        User sender = ctx.getUser();
        long userId = sender.getIdLong();
        int color = input == null ? 0 : Bot.hexToRgb(input);

        try {
            CustomUserinfo csInfo = csinfoRepo.findByUserId(userId, CustomUserinfo.fromUserId(userId))
                    .setColor(color)
                    .tickUpdate();

            csinfoRepo.upsert(csInfo);
            return Status.USERINFO_COLOR_SUCCESSFULLY_UPDATED;
        } catch (DataAccessException e) {
            LOGGER.error("Could not update userinfo color of user '{}'", userId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        } catch (NumberFormatException e) {
            return Status.INVALID_HEX_PROVIDED;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Define/reseta a cor da barra lateral da embed apresentada no userinfo.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "color", "A cor em HEX a reset definida. Ignore para remover.")
                        .setRequiredLength(6, 6)
        );
    }
}