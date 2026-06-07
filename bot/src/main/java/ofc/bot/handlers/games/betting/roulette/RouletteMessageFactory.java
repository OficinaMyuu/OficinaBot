package ofc.bot.handlers.games.betting.roulette;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.util.Bot;
import ofc.bot.util.embeds.EmbedFactory;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates Discord embeds for the channel-scoped roulette betting flow.
 * <p>
 * This factory keeps all player-facing roulette copy and field grouping in one
 * place so the command and game lifecycle can focus on state transitions,
 * settlement, and persistence.
 */
public final class RouletteMessageFactory {
    private static final int MAX_DISPLAYED_BETS = 12;

    /**
     * Prevents instantiation because all embed builders are stateless factories.
     */
    private RouletteMessageFactory() {}

    /**
     * Builds the live lobby embed shown while bets are still open.
     *
     * @param snapshot immutable view of the current lobby state
     * @return an embed containing the countdown, accepted bets, total stake, and
     * distinct player count
     */
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

    /**
     * Builds the final spin result embed.
     * <p>
     * Winning and losing resolved entries are rendered in separate fields. Empty
     * groups are omitted, which keeps all-win and all-loss spins concise while
     * still showing the landed roulette space in the description.
     *
     * @param result resolved spin and all accepted bets
     * @return an embed colored by whether at least one payout was produced
     */
    public static MessageEmbed result(RouletteResult result) {
        RouletteSpin spin = result.spin();
        boolean hasWinners = !result.payoutsByUser().isEmpty();
        Color color = hasWinners ? EmbedFactory.OK_GREEN : EmbedFactory.DANGER_RED;
        List<RouletteResolvedEntry> winners = result.entries().stream()
                .filter(RouletteResolvedEntry::won)
                .toList();
        List<RouletteResolvedEntry> losers = result.entries().stream()
                .filter(entry -> !entry.won())
                .toList();

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Roleta - Resultado")
                .setDescription("A bola caiu em `" + spin.number() + "` (" + spin.color().displayName() + ").")
                .setColor(color);

        addResolvedField(builder, "Vencedores", winners);
        addResolvedField(builder, "Perdedores", losers);

        return builder.build();
    }

    /**
     * Renders accepted lobby entries newest-first with their stake and multiplier.
     *
     * @param entries accepted bets in the lobby
     * @return a Discord embed field value capped to the platform limit
     */
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

    /**
     * Adds a resolved result field when that group has at least one entry.
     *
     * @param builder embed builder receiving the field
     * @param name field title shown to users
     * @param entries resolved entries in the group
     */
    private static void addResolvedField(EmbedBuilder builder, String name, List<RouletteResolvedEntry> entries) {
        if (entries.isEmpty()) return;

        builder.addField(name, formatResolvedEntries(entries), false);
    }

    /**
     * Renders either winning or losing resolved entries for the final result.
     *
     * @param entries resolved entries that all share the same outcome group
     * @return a Discord embed field value capped to the platform limit
     */
    private static String formatResolvedEntries(List<RouletteResolvedEntry> entries) {
        String rendered = entries.stream()
                .limit(MAX_DISPLAYED_BETS)
                .map(resolved -> {
                    RouletteEntry entry = resolved.entry();
                    String result = resolved.won()
                            ? "ganhou " + Bot.fmtMoney(resolved.payout())
                            : "perdeu " + Bot.fmtMoney(entry.amount());
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

    /**
     * Counts unique lobby participants regardless of how many bets they placed.
     *
     * @param entries accepted bets in the lobby
     * @return distinct user count
     */
    private static long countPlayers(List<RouletteEntry> entries) {
        return entries.stream().map(RouletteEntry::userId).distinct().count();
    }
}
