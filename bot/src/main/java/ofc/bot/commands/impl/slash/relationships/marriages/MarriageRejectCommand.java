package ofc.bot.commands.impl.slash.relationships.marriages;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.MarriageRequest;
import ofc.bot.domain.sqlite.repository.MarriageRequestRepository;
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

@DiscordCommand(name = "marriage reject")
public class MarriageRejectCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarriageRejectCommand.class);
    private final MarriageRequestRepository mreqRepo;

    public MarriageRejectCommand(MarriageRequestRepository mreqRepo) {
        this.mreqRepo = mreqRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User issuer = ctx.getUser();
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        long issuerId = issuer.getIdLong();
        long targetId = target.getIdLong();
        MarriageRequest req = mreqRepo.findByStrictIds(targetId, issuerId);

        if (req == null)
            return Status.NO_INCOME_PROPOSAL_FROM_USER.args(target.getAsMention());

        try {
            mreqRepo.delete(req);

            return Status.MARRIAGE_PROPOSAL_REJECTED_SUCCESSFULLY;
        } catch (DataAccessException e) {
            LOGGER.error("Could not reject proposal of '{}' and '{}'", issuerId, targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Recuse uma proposta de casamento.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "A pessoa na qual você quer recusar o pedido de casamento.", true)
        );
    }
}