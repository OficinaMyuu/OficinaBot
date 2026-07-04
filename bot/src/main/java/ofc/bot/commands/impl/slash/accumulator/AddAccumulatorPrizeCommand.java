package ofc.bot.commands.impl.slash.accumulator;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.database.repository.AccumulatorPrizeRepository;
import ofc.bot.handlers.accumulator.AccumulatorMessageFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "accumulator add")
public class AddAccumulatorPrizeCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddAccumulatorPrizeCommand.class);
    private final AccumulatorPrizeRepository prizeRepo;

    public AddAccumulatorPrizeCommand(AccumulatorPrizeRepository prizeRepo) {
        this.prizeRepo = prizeRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        AccumulatorPrizeConfig.ParseResult configResult = AccumulatorPrizeConfig.parse(ctx);
        if (configResult.failed()) {
            return ctx.replyEmbeds(true, configResult.failure());
        }

        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        if (target == null) {
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Usuário inválido",
                    "Escolha o usuário que deve receber o prêmio."
            ));
        }

        AccumulatorPrizeConfig config = configResult.config();
        long now = Bot.unixNow();
        long guildId = ctx.getGuildId();
        long createdBy = ctx.getUserId();
        AccumulatorPrize prize = config.createPrize(guildId, target.getIdLong(), createdBy, now);

        try {
            prizeRepo.save(prize);
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.addSuccess(ctx.getUser(), target, config.type()));
        } catch (DataAccessException e) {
            LOGGER.error("Could not accumulate {} prize for user {} in guild {}", config.type(), target.getIdLong(), guildId, e);
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Não foi possível acumular o prêmio",
                    "A caixa de prêmios pendentes não pôde ser atualizada."
            ));
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Adds event prizes to the pending accumulator box.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return AccumulatorPrizeConfig.options(new OptionData(OptionType.USER, "user", "Winner to add.", true));
    }
}
