package ofc.bot.handlers.giveaway;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.GiveawayWinner;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.util.Bot;
import ofc.bot.util.embeds.EmbedFactory;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public final class GiveawayMessageFactory {
    private GiveawayMessageFactory() {}

    public static MessageEmbed activeGiveaway(Giveaway giveaway, int entries) {
        EmbedBuilder builder = new EmbedBuilder();
        String description = giveaway.getDescription();

        builder.setTitle(giveaway.getTitle())
                .setColor(Bot.Colors.DEFAULT)
                .setDescription(description == null || description.isBlank() ? null : description)
                .addField("🎁 Prêmio", formatPrize(giveaway), true)
                .addField("👥 Participantes", Bot.fmtNum(entries), true)
                .addField("🏆 Vencedores", Bot.fmtNum(giveaway.getWinnerCount()), true)
                .addField("⏰ Termina", String.format("<t:%d:F>\n<t:%d:R>", giveaway.getEndsAt(), giveaway.getEndsAt()), true)
                .addField("👑 Host", String.format("<@%d>", giveaway.getHostId()), true)
                .setFooter("ID: " + giveaway.getGiveawayId());

        if (giveaway.getRequiredVoiceChannelId() != null) {
            builder.addField("🔊 Requer Participação", String.format("<#%d>", giveaway.getRequiredVoiceChannelId()), true);
        }

        return builder.build();
    }

    public static MessageEmbed endedGiveaway(Giveaway giveaway, int entries, List<GiveawayWinner> winners) {
        EmbedBuilder builder = new EmbedBuilder();
        String winnerText = winners.isEmpty()
                ? "Nenhum vencedor. Ninguém participou."
                : winners.stream()
                        .map(winner -> String.format("<@%d>", winner.getUserId()))
                        .collect(Collectors.joining(", "));

        return builder.setTitle(giveaway.getTitle())
                .setColor(winners.isEmpty() ? EmbedFactory.DANGER_RED : EmbedFactory.OK_GREEN)
                .setDescription("Sorteio encerrado.")
                .addField("🎁 Prêmio", formatPrize(giveaway), true)
                .addField("👥 Participantes", Bot.fmtNum(entries), true)
                .addField("🏆 Vencedores", winnerText, false)
                .setFooter("ID: " + giveaway.getGiveawayId())
                .build();
    }

    public static MessageEmbed endedAnnouncement(Giveaway giveaway, List<GiveawayWinner> winners, boolean reroll) {
        String winnerText = winners.isEmpty()
                ? "Nenhum vencedor foi encontrado."
                : winners.stream()
                        .map(winner -> String.format("<@%d>", winner.getUserId()))
                        .collect(Collectors.joining(", "));
        String title = reroll ? "Novos vencedores do sorteio" : "Sorteio encerrado";

        return new EmbedBuilder()
                .setTitle(title)
                .setColor(winners.isEmpty() ? Color.YELLOW : EmbedFactory.OK_GREEN)
                .setDescription(winnerText)
                .addField("🎁 Prêmio", formatPrize(giveaway), false)
                .setFooter("ID: " + giveaway.getGiveawayId())
                .build();
    }

    public static MessageEmbed claimPrompt(Giveaway giveaway) {
        String body = switch (giveaway.getPrizeType()) {
            case ECONOMY_MONEY -> "Escolha em qual economia você quer receber o dinheiro.";
            case COLOR_ROLE -> "Escolha qual cargo de cor você quer receber.";
            case GENERIC -> "Esse prêmio deve ser entregue manualmente pelo host.";
        };

        return new EmbedBuilder()
                .setTitle("Resgate do Sorteio")
                .setColor(Bot.Colors.DEFAULT)
                .setDescription(body)
                .addField("🎁 Prêmio", formatPrize(giveaway), false)
                .build();
    }

    public static MessageEmbed claimSuccess(String message) {
        return new EmbedBuilder()
                .setTitle("Prêmio entregue")
                .setColor(EmbedFactory.OK_GREEN)
                .setDescription(message)
                .build();
    }

    public static MessageEmbed claimFailure(String message) {
        return new EmbedBuilder()
                .setTitle("Não foi possível resgatar")
                .setColor(EmbedFactory.DANGER_RED)
                .setDescription(message)
                .build();
    }

    private static String formatPrize(Giveaway giveaway) {
        GiveawayPrizeType type = giveaway.getPrizeType();
        return switch (type) {
            case GENERIC -> giveaway.getDescription() == null || giveaway.getDescription().isBlank()
                    ? "Prêmio genérico"
                    : giveaway.getDescription();
            case ECONOMY_MONEY -> Bot.fmtMoney(giveaway.getMoneyAmount() == null ? 0 : giveaway.getMoneyAmount());
            case COLOR_ROLE -> {
                Long seconds = giveaway.getColorRoleDurationSeconds();
                String duration = seconds == null ? "sem duração definida" : Bot.parsePeriod(seconds);
                yield "Cargo de cor por " + duration;
            }
        };
    }
}
