package ofc.bot.handlers.games.betting.blackjack;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.util.Bot;
import ofc.bot.util.embeds.EmbedFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlackjackMessageFactoryTest {
    private static final BlackjackPlayerView PLAYER = new BlackjackPlayerView(1L, "leeo13", null);

    @Test
    void activeEmbedShouldHideDealerHoleCardInPortugueseInterface() {
        BlackjackRound round = new BlackjackRound(1_000, BlackjackShoe.fixed(
                BlackjackCard.SIX_OF_HEARTS,
                BlackjackCard.TEN_OF_HEARTS,
                BlackjackCard.THREE_OF_SPADES,
                BlackjackCard.NINE_OF_CLUBS
        ));

        MessageEmbed embed = BlackjackMessageFactory.active(PLAYER, round);
        MessageEmbed.Field dealer = field(embed, "Mão da Banca");

        assertEquals(Bot.Colors.DEFAULT, embed.getColor());
        assertEquals(List.of("Sua Mão", "Mão da Banca"), fieldNames(embed));
        assertEquals("Cartas restantes: " + round.cardsRemaining(), embed.getFooter().getText());
        assertTrue(dealer.getValue().contains(BlackjackCard.backDisplay()));
        assertFalse(dealer.getValue().contains(BlackjackCard.NINE_OF_CLUBS.display()));
        assertTrue(field(embed, "Sua Mão").getValue().contains("Valor: 9"));
    }

    @Test
    void resultEmbedShouldRevealDealerAndUseOutcomeColor() {
        BlackjackRound round = new BlackjackRound(1_000, BlackjackShoe.fixed(
                BlackjackCard.SIX_OF_HEARTS,
                BlackjackCard.TEN_OF_HEARTS,
                BlackjackCard.THREE_OF_SPADES,
                BlackjackCard.NINE_OF_CLUBS
        ));
        round.stand();

        MessageEmbed embed = BlackjackMessageFactory.result(PLAYER, round);

        assertEquals(EmbedFactory.DANGER_RED, embed.getColor());
        assertTrue(embed.getDescription().contains("Resultado: Derrota"));
        assertTrue(field(embed, "Mão da Banca").getValue().contains(BlackjackCard.NINE_OF_CLUBS.display()));
        assertEquals(List.of("Sua Mão", "Mão da Banca"), fieldNames(embed));
        assertEquals("Cartas restantes: " + round.cardsRemaining(), embed.getFooter().getText());
    }

    @Test
    void shouldFormatSignedMoney() {
        assertEquals("+$1.000", BlackjackMessageFactory.formatSignedMoney(1_000));
        assertEquals("-$1.000", BlackjackMessageFactory.formatSignedMoney(-1_000));
        assertEquals("$0", BlackjackMessageFactory.formatSignedMoney(0));
    }

    private List<String> fieldNames(MessageEmbed embed) {
        return embed.getFields().stream()
                .map(MessageEmbed.Field::getName)
                .toList();
    }

    private MessageEmbed.Field field(MessageEmbed embed, String name) {
        return embed.getFields().stream()
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
    }
}
