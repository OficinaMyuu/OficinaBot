package ofc.bot.handlers.games.betting.blackjack;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.util.Bot;
import ofc.bot.util.embeds.EmbedFactory;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public final class BlackjackMessageFactory {
    private BlackjackMessageFactory() {}

    public static MessageEmbed active(BlackjackPlayerView player, BlackjackRound round) {
        return base(player, round, false)
                .setColor(Bot.Colors.DEFAULT)
                .build();
    }

    public static MessageEmbed result(BlackjackPlayerView player, BlackjackRound round) {
        int net = net(round.resolvedHands());
        return base(player, round, true)
                .setColor(resultColor(net))
                .setDescription("Resultado: " + resultLabel(net) + " `" + formatSignedMoney(net) + "`")
                .build();
    }

    private static EmbedBuilder base(BlackjackPlayerView player, BlackjackRound round, boolean revealDealer) {
        return new EmbedBuilder()
                .setAuthor(player.name(), null, player.avatarUrl())
                .addField("Sua Mão", formatPlayerHands(round), true)
                .addField("Mão da Banca", formatDealerHand(round, revealDealer), true)
                .setFooter("Cartas restantes: " + round.cardsRemaining());
    }

    private static String formatPlayerHands(BlackjackRound round) {
        List<BlackjackHand> hands = round.playerHands();
        if (hands.size() == 1) {
            return formatHand(hands.getFirst());
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hands.size(); i++) {
            BlackjackHand hand = hands.get(i);
            boolean active = !round.isSettled() && i == round.activeHandIndex();
            builder.append("**Mão ").append(i + 1);
            if (active) {
                builder.append(" (ativa)");
            }
            builder.append("**\n")
                    .append(formatHand(hand));
            if (i < hands.size() - 1) {
                builder.append("\n\n");
            }
        }
        return builder.toString();
    }

    private static String formatDealerHand(BlackjackRound round, boolean revealDealer) {
        BlackjackHand dealer = round.dealer();
        if (revealDealer) {
            return formatHand(dealer);
        }

        BlackjackCard upcard = dealer.cards().getFirst();
        BlackjackHand visible = new BlackjackHand(0, false);
        visible.add(upcard);
        return upcard.display() + " " + BlackjackCard.backDisplay()
                + "\n\nValor: " + visible.value().display();
    }

    private static String formatHand(BlackjackHand hand) {
        return hand.displayCards() + "\n\nValor: " + hand.value().display();
    }

    private static String formatResolvedHands(List<BlackjackResolvedHand> resolvedHands) {
        if (resolvedHands.size() == 1) {
            BlackjackResolvedHand resolved = resolvedHands.getFirst();
            return resolved.outcome().displayName() + " `" + formatSignedMoney(resolved.net()) + "`";
        }

        return resolvedHands.stream()
                .map(resolved -> String.format(
                        "Mão %d: %s `%s`",
                        resolvedHands.indexOf(resolved) + 1,
                        resolved.outcome().displayName(),
                        formatSignedMoney(resolved.net())
                ))
                .collect(Collectors.joining("\n"));
    }

    private static int net(List<BlackjackResolvedHand> resolvedHands) {
        return resolvedHands.stream().mapToInt(BlackjackResolvedHand::net).sum();
    }

    private static Color resultColor(int net) {
        if (net > 0) return EmbedFactory.OK_GREEN;
        if (net < 0) return EmbedFactory.DANGER_RED;
        return Bot.Colors.DEFAULT;
    }

    private static String resultLabel(int net) {
        if (net > 0) return BlackjackOutcome.WIN.displayName();
        if (net < 0) return BlackjackOutcome.LOSS.displayName();
        return BlackjackOutcome.PUSH.displayName();
    }

    static String formatSignedMoney(int value) {
        if (value > 0) return "+" + Bot.fmtMoney(value);
        if (value < 0) return "-" + Bot.fmtMoney(Math.abs(value));
        return Bot.fmtMoney(0);
    }
}
