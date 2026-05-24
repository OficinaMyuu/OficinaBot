package ofc.bot.commands.impl.slash.relationships;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.Main;
import ofc.bot.domain.entity.Marriage;
import ofc.bot.domain.sqlite.repository.MarriageRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "divorce")
public class DivorceCommand extends SlashCommand {
    private static final long JANJO = 742729586659295283L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DivorceCommand.class);
    private final MarriageRepository marrRepo;

    public DivorceCommand(MarriageRepository marrRepo) {
        this.marrRepo = marrRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User sender = ctx.getUser();
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        String requesterName = sender.getEffectiveName();
        long requesterId = sender.getIdLong();
        long targetId = target.getIdLong();

        if (targetId == requesterId)
            return Status.CANNOT_DIVORCE_YOURSELF;

        Marriage relationship = marrRepo.findByUserIds(requesterId, targetId);

        if (relationship == null)
            return Status.USER_IS_NOT_MARRIED_TO_TARGET.args(target.getAsMention());

        try {
            marrRepo.delete(relationship);

            if (targetId != JANJO)
                informTarget(requesterName, targetId);

            return Status.DIVORCED_SUCCESSFULLY.setEphm(true);
        } catch (DataAccessException e) {
            LOGGER.error("Could not divorce '{}' and '{}'", requesterId, targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Divorcie-se de algum usuário.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a se divorciar.", true)
        );
    }

    private void informTarget(String requesterName, long targetId) {
        Main.getApi().openPrivateChannelById(targetId)
                .flatMap((dm) -> dm.sendMessageFormat("O seu casamento com %s infelizmente chegou ao fim. 😔", requesterName))
                .queue(null, new ErrorHandler().ignore(ErrorResponse.CANNOT_SEND_TO_USER));
    }
}