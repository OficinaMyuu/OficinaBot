package ofc.bot.handlers.games.betting.roulette;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.util.Bot;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RouletteMessageFactory {
    private static final int MAX_DISPLAYED_BETS = 12;

    private RouletteMessageFactory() {}

    public static MessageEmbed lobby(RouletteGameSnapshot snapshot) {
        List<RouletteEntry> entries = snapshot.entries();
        int total = entries.stream().mapToInt(RouletteEntry::amount).sum();
        String remaining = snapshot.endsAt() > 0 ? "<t:" + snapshot.endsAt() + ":R>" : "em 30 segundos";

        return new EmbedBuilder()
                .setTitle("Roleta")
                .setDescription("Apostas abertas. A bola cai " + remaining + ".")
                .setColor(Bot.Colors.DEFAULT)
                .addField("Apostas", formatEntries(entries), false)
                .addField("Total apostado", Bot.fmtMoney(total), true)
                .addField("Jogadores", String.valueOf(countPlayers(entries)), true)
                .build();
    }

    public static MessageEmbed result(RouletteResult result) {
        RouletteSpin spin = result.spin();
        Map<Long, Integer> payouts = result.payoutsByUser();
        Color color = payouts.isEmpty() ? Color.RED : Color.GREEN;

        return new EmbedBuilder()
                .setTitle("Roleta - Resultado")
                .setDescription("A bola caiu em `" + spin.number() + "` (" + spin.color().displayName() + ").")
                .setColor(color)
                .addField("Vencedores", formatWinners(payouts), false)
                .addField("Apostas", formatResolvedEntries(result.entries()), false)
                .build();
    }

    private static String formatEntries(List<RouletteEntry> entries) {
        if (entries.isEmpty()) return "Nenhuma aposta.";

        String rendered = entries.stream()
                .sorted(Comparator.comparingLong(RouletteEntry::createdAt).reversed())
                .limit(MAX_DISPLAYED_BETS)
                .map(entry -> String.format(
                        "<@%d> apostou %s em `%s` (x%d)",
                        entry.userId(),
                        Bot.fmtMoney(entry.amount()),
                        entry.bet().canonicalName(),
                        entry.bet().multiplier()
                ))
                .collect(Collectors.joining("\n"));

        int remaining = entries.size() - MAX_DISPLAYED_BETS;
        if (remaining > 0) {
            rendered += "\n... e mais " + remaining + " aposta(s).";
        }
        return Bot.limitStr(rendered, MessageEmbed.VALUE_MAX_LENGTH);
    }

    private static String formatResolvedEntries(List<RouletteResolvedEntry> entries) {
        if (entries.isEmpty()) return "Nenhuma aposta.";

        String rendered = entries.stream()
                .limit(MAX_DISPLAYED_BETS)
                .map(resolved -> {
                    RouletteEntry entry = resolved.entry();
                    String result = resolved.won() ? "ganhou " + Bot.fmtMoney(resolved.payout()) : "perdeu";
                    return String.format(
                            "<@%d> `%s` %s",
                            entry.userId(),
                            entry.bet().canonicalName(),
                            result
                    );
                })
                .collect(Collectors.joining("\n"));

        int remaining = entries.size() - MAX_DISPLAYED_BETS;
        if (remaining > 0) {
            rendered += "\n... e mais " + remaining + " aposta(s).";
        }
        return Bot.limitStr(rendered, MessageEmbed.VALUE_MAX_LENGTH);
    }

    private static String formatWinners(Map<Long, Integer> payouts) {
        if (payouts.isEmpty()) return "Ninguém venceu desta vez.";

        return payouts.entrySet()
                .stream()
                .map(entry -> String.format("<@%d> recebeu %s", entry.getKey(), Bot.fmtMoney(entry.getValue())))
                .collect(Collectors.joining("\n"));
    }

    private static long countPlayers(List<RouletteEntry> entries) {
        return entries.stream().map(RouletteEntry::userId).distinct().count();
    }
}
