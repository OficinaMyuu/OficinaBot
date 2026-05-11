package ofc.bot.commands.impl.slash.giveaway;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.handlers.giveaway.GiveawayInputParser;
import ofc.bot.handlers.giveaway.GiveawayModalFactory;
import ofc.bot.handlers.giveaway.GiveawayModalFactory.GiveawayCreateDraft;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "giveaway create")
public class CreateGiveawayCommand extends SlashSubcommand {
    private static final int MIN_DURATION_SECONDS = 10;
    private static final int MAX_WINNERS = 25;

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String typeOption = ctx.getSafeOption("type", OptionMapping::getAsString);
        GiveawayPrizeType prizeType = GiveawayPrizeType.fromOption(typeOption);
        if (prizeType == null) {
            return ctx.reply("Tipo de prêmio inválido.", true);
        }

        String title = ctx.getSafeOption("title", OptionMapping::getAsString).strip();
        int winnerCount = ctx.getSafeOption("winners", OptionMapping::getAsInt);
        String durationInput = ctx.getSafeOption("duration", OptionMapping::getAsString);
        long durationSeconds = GiveawayInputParser.parseDurationSeconds(durationInput);
        GuildChannel channel = ctx.getSafeOption("channel", OptionMapping::getAsChannel);
        GuildChannel requiredVoice = ctx.getOption("required-vc", null, OptionMapping::getAsChannel);

        if (durationSeconds < MIN_DURATION_SECONDS) {
            return ctx.reply("Duração inválida. Use pelo menos 10s. Exemplo: `1h`, `30m`, `7d`.", true);
        }

        if (winnerCount <= 0 || winnerCount > MAX_WINNERS) {
            return ctx.reply("A quantidade de vencedores precisa estar entre 1 e 25.", true);
        }

        if (channel.getType() != ChannelType.TEXT) {
            return ctx.reply("O canal do sorteio precisa ser um canal de texto.", true);
        }

        if (requiredVoice != null && requiredVoice.getType() != ChannelType.VOICE) {
            return ctx.reply("A condição de voz precisa ser um canal de voz.", true);
        }

        GiveawayCreateDraft draft = new GiveawayCreateDraft(
                ctx.getGuildId(),
                ctx.getUserId(),
                channel.getIdLong(),
                prizeType,
                Bot.limitStr(title, 256),
                winnerCount,
                Bot.unixNow() + durationSeconds,
                requiredVoice == null ? null : requiredVoice.getIdLong()
        );

        return ctx.replyModal(GiveawayModalFactory.createPrizeModal(draft));
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cria um sorteio.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "type", "Tipo do prêmio.", true)
                        .addChoice("Genérico", GiveawayPrizeType.GENERIC.name())
                        .addChoice("Dinheiro", GiveawayPrizeType.ECONOMY_MONEY.name())
                        .addChoice("Cargo de cor", GiveawayPrizeType.COLOR_ROLE.name()),
                new OptionData(OptionType.STRING, "title", "Título do sorteio.", true)
                        .setRequiredLength(1, 256),
                new OptionData(OptionType.INTEGER, "winners", "Quantidade de vencedores.", true)
                        .setRequiredRange(1, MAX_WINNERS),
                new OptionData(OptionType.STRING, "duration", "Duração. Exemplos: 30m, 2h, 7d.", true)
                        .setRequiredLength(1, 40),
                new OptionData(OptionType.CHANNEL, "channel", "Canal onde o sorteio será publicado.", true)
                        .setChannelTypes(ChannelType.TEXT),
                new OptionData(OptionType.CHANNEL, "required-vc", "Canal de voz obrigatório para participar.")
                        .setChannelTypes(ChannelType.VOICE)
        );
    }
}
