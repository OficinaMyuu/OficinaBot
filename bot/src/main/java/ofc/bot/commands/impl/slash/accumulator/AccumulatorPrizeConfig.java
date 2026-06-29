package ofc.bot.commands.impl.slash.accumulator;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.handlers.accumulator.AccumulatorMessageFactory;
import ofc.bot.handlers.giveaway.GiveawayInputParser;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;

import java.util.List;

record AccumulatorPrizeConfig(
        AccumulatorPrizeType type,
        Integer amount,
        Long colorDurationSeconds
) {
    static ParseResult parse(SlashCommandContext ctx) {
        AccumulatorPrizeType type = ctx.getSafeEnumOption("type", AccumulatorPrizeType.class);
        Integer amount = ctx.getOption("amount", OptionMapping::getAsInt);
        String durationInput = ctx.getOption("duration", OptionMapping::getAsString);

        if (type == AccumulatorPrizeType.MONEY) {
            if (!AccumulatorPrize.isValidAmount(amount)) {
                return ParseResult.failure(AccumulatorMessageFactory.failure(
                        "Valor inválido",
                        "Prêmios em dinheiro devem ter valor entre 1 e " + AccumulatorPrize.MAX_AMOUNT + "."
                ));
            }
            return ParseResult.success(new AccumulatorPrizeConfig(type, amount, null));
        }

        long duration = GiveawayInputParser.parseDurationSeconds(durationInput);
        if (duration <= 0) {
            return ParseResult.failure(AccumulatorMessageFactory.failure(
                    "Duração inválida",
                    "Prêmios de cargo de cor precisam de uma duração válida. Exemplos: `7d`, `30d`, `2h`."
            ));
        }
        return ParseResult.success(new AccumulatorPrizeConfig(type, null, duration));
    }

    AccumulatorPrize createPrize(long guildId, long targetId, long createdBy, long createdAt) {
        return new AccumulatorPrize(
                guildId,
                targetId,
                createdBy,
                type,
                type == AccumulatorPrizeType.MONEY ? amount : null,
                colorDurationSeconds,
                createdAt
        );
    }

    static List<OptionData> options(OptionData targetOption) {
        return List.of(
                new OptionData(OptionType.STRING, "type", "Prize type.", true)
                        .addChoice(AccumulatorPrizeType.MONEY.getDisplay(), AccumulatorPrizeType.MONEY.name())
                        .addChoice(AccumulatorPrizeType.COLOR_ROLE.getDisplay(), AccumulatorPrizeType.COLOR_ROLE.name()),
                targetOption,
                new OptionData(OptionType.INTEGER, "amount", "Money amount.")
                        .setRequiredRange(1, AccumulatorPrize.MAX_AMOUNT),
                new OptionData(OptionType.STRING, "duration", "Color role duration. Examples: 7d, 30d, 2h.")
                        .setRequiredLength(1, 40)
        );
    }

    record ParseResult(AccumulatorPrizeConfig config, MessageEmbed failure) {
        static ParseResult success(AccumulatorPrizeConfig config) {
            return new ParseResult(config, null);
        }

        static ParseResult failure(MessageEmbed failure) {
            return new ParseResult(null, failure);
        }

        boolean failed() {
            return failure != null;
        }
    }
}
