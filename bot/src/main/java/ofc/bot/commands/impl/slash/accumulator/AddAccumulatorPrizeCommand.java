package ofc.bot.commands.impl.slash.accumulator;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.sqlite.repository.AccumulatorPrizeRepository;
import ofc.bot.handlers.accumulator.AccumulatorInputParser;
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
        User user = ctx.getOption("user", OptionMapping::getAsUser);
        String entries = ctx.getOption("entries", OptionMapping::getAsString);
        Integer amount = ctx.getOption("amount", OptionMapping::getAsInt);
        String durationInput = ctx.getOption("duration", OptionMapping::getAsString);

        if (type == AccumulatorPrizeType.MONEY && amount != null && !AccumulatorInputParser.isValidAmount(amount)) {
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Invalid amount",
                    "Money prizes must be between 1 and " + AccumulatorPrize.MAX_AMOUNT + "."
            ));
        }

        long duration = 0;
        if (type == AccumulatorPrizeType.COLOR_ROLE) {
            duration = GiveawayInputParser.parseDurationSeconds(durationInput);
            if (duration <= 0) {
                return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                        "Invalid duration",
                        "Color role prizes need a valid duration. Examples: `7d`, `30d`, `2h`."
                ));
            }
        }

        AccumulatorInputParser.Result parsed = AccumulatorInputParser.parse(type, user, entries, amount);
        if (!parsed.isOk()) {
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Could not add prizes",
                    Bot.limitStr(String.join("\n", parsed.errors()), 3500)
            ));
        }

        long now = Bot.unixNow();
        long guildId = ctx.getGuildId();
        long createdBy = ctx.getUserId();
        Long colorDuration = type == AccumulatorPrizeType.COLOR_ROLE ? duration : null;
        List<AccumulatorPrize> prizes = parsed.entries().stream()
                .map(entry -> new AccumulatorPrize(
                        guildId,
                        entry.userId(),
                        createdBy,
                        type,
                        type == AccumulatorPrizeType.MONEY ? entry.amount() : null,
                        colorDuration,
                        now
                ))
                .toList();

        try {
            prizeRepo.bulkSave(prizes);
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.addSuccess(ctx.getUser(), type, prizes.size()));
        } catch (DataAccessException e) {
            LOGGER.error("Could not accumulate {} prize(s) in guild {}", prizes.size(), guildId, e);
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Could not add prizes",
                    "The pending box could not be updated."
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
                new OptionData(OptionType.USER, "user", "Single winner to add."),
                new OptionData(OptionType.STRING, "entries", "Bulk entries: one user id per line, or `<user> <amount>` for money.")
                        .setRequiredLength(1, 4000),
                new OptionData(OptionType.INTEGER, "amount", "Money amount used by single entries or as the bulk default.")
                        .setRequiredRange(1, AccumulatorPrize.MAX_AMOUNT),
                new OptionData(OptionType.STRING, "duration", "Color role duration. Examples: 7d, 30d, 2h.")
                        .setRequiredLength(1, 40)
        );
    }
}
