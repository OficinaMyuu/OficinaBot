package ofc.bot.handlers.games.betting.roulette;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.util.embeds.EmbedFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the public embed contract for roulette lobby settlement results.
 */
class RouletteMessageFactoryTest {
    /**
     * Ensures mixed spins render only winner and loser result groups, never the
     * previous combined all-bets field.
     */
    @Test
    void resultEmbedSplitsWinnersAndLosers() {
        MessageEmbed embed = result(
                RouletteSpin.of(3),
                List.of(
                        entry(1L, "odd", 100),
                        entry(2L, "black", 200)
                )
        );

        List<String> names = fieldNames(embed);

        assertEquals(EmbedFactory.OK_GREEN, embed.getColor());
        assertEquals(List.of("Vencedores", "Perdedores"), names);
        assertFalse(names.contains("Apostas"));
        assertTrue(field(embed, "Vencedores").getValue().contains("<@1> `odd` ganhou"));
        assertTrue(field(embed, "Perdedores").getValue().contains("<@2> `black` perdeu"));
    }

    /**
     * Ensures all-loss spins use the shared danger color and omit the empty
     * winners field.
     */
    @Test
    void resultEmbedOmitsWinnersWhenEveryoneLoses() {
        MessageEmbed embed = result(
                RouletteSpin.of(2),
                List.of(entry(1L, "red", 100))
        );

        assertEquals(EmbedFactory.DANGER_RED, embed.getColor());
        assertEquals(List.of("Perdedores"), fieldNames(embed));
    }

    /**
     * Ensures all-win spins use the shared success color and omit the empty
     * losers field.
     */
    @Test
    void resultEmbedOmitsLosersWhenEveryoneWins() {
        MessageEmbed embed = result(
                RouletteSpin.of(2),
                List.of(
                        entry(1L, "black", 100),
                        entry(2L, "even", 200)
                )
        );

        assertEquals(EmbedFactory.OK_GREEN, embed.getColor());
        assertEquals(List.of("Vencedores"), fieldNames(embed));
    }

    /**
     * Resolves a result embed from a spin and accepted entries.
     *
     * @param spin landed roulette space
     * @param entries accepted bets to resolve
     * @return final result embed
     */
    private MessageEmbed result(RouletteSpin spin, List<RouletteEntry> entries) {
        return RouletteMessageFactory.result(RouletteResult.resolve(spin, entries));
    }

    /**
     * Creates a roulette entry with a parsed canonical bet.
     *
     * @param userId Discord user id placing the bet
     * @param bet supported roulette bet string
     * @param amount stake charged from the bank
     * @return immutable roulette entry for tests
     */
    private RouletteEntry entry(long userId, String bet, int amount) {
        return new RouletteEntry(userId, RouletteBet.parse(bet).orElseThrow(), amount, 1L);
    }

    /**
     * Extracts field names in rendered order for concise assertions.
     *
     * @param embed embed under test
     * @return ordered field names
     */
    private List<String> fieldNames(MessageEmbed embed) {
        return embed.getFields().stream()
                .map(MessageEmbed.Field::getName)
                .toList();
    }

    /**
     * Finds a field by exact name.
     *
     * @param embed embed under test
     * @param name field name to locate
     * @return matching field
     */
    private MessageEmbed.Field field(MessageEmbed embed, String name) {
        return embed.getFields().stream()
                .filter(candidate -> name.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
    }
}
