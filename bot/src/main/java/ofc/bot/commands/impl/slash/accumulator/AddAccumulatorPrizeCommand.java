package ofc.bot.commands.impl.slash.accumulator;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.sqlite.repository.AccumulatorPrizeRepository;
import ofc.bot.handlers.accumulator.AccumulatorMessageFactory;
import ofc.bot.handlers.giveaway.GiveawayInputParser;
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
        AccumulatorPrizeType type = ctx.getSafeEnumOption("type", AccumulatorPrizeType.class);
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        Integer amount = ctx.getOption("amount", OptionMapping::getAsInt);
        String durationInput = ctx.getOption("duration", OptionMapping::getAsString);

        if (target == null) {
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Usuário inválido",
                    "Escolha o usuário que deve receber o prêmio."
            ));
        }

        if (type == AccumulatorPrizeType.MONEY && !AccumulatorPrize.isValidAmount(amount)) {
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Valor inválido",
                    "Prêmios em dinheiro devem ter valor entre 1 e " + AccumulatorPrize.MAX_AMOUNT + "."
            ));
        }

        long duration = 0;
        if (type == AccumulatorPrizeType.COLOR_ROLE) {
            duration = GiveawayInputParser.parseDurationSeconds(durationInput);
            if (duration <= 0) {
                return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                        "Duração inválida",
                        "Prêmios de cargo de cor precisam de uma duração válida. Exemplos: `7d`, `30d`, `2h`."
                ));
            }
        }

        long now = Bot.unixNow();
        long guildId = ctx.getGuildId();
        long createdBy = ctx.getUserId();
        Long colorDuration = type == AccumulatorPrizeType.COLOR_ROLE ? duration : null;
        AccumulatorPrize prize = new AccumulatorPrize(
                guildId,
                target.getIdLong(),
                createdBy,
                type,
                type == AccumulatorPrizeType.MONEY ? amount : null,
                colorDuration,
                now
        );

        try {
            prizeRepo.save(prize);
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.addSuccess(ctx.getUser(), target, type));
        } catch (DataAccessException e) {
            LOGGER.error("Could not accumulate {} prize for user {} in guild {}", type, target.getIdLong(), guildId, e);
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
        return List.of(
                new OptionData(OptionType.STRING, "type", "Prize type.", true)
                        .addChoice(AccumulatorPrizeType.MONEY.getDisplay(), AccumulatorPrizeType.MONEY.name())
                        .addChoice(AccumulatorPrizeType.COLOR_ROLE.getDisplay(), AccumulatorPrizeType.COLOR_ROLE.name()),
                new OptionData(OptionType.USER, "user", "Winner to add.", true),
                new OptionData(OptionType.INTEGER, "amount", "Money amount.")
                        .setRequiredRange(1, AccumulatorPrize.MAX_AMOUNT),
                new OptionData(OptionType.STRING, "duration", "Color role duration. Examples: 7d, 30d, 2h.")
                        .setRequiredLength(1, 40)
        );
    }
}
