package ofc.bot.handlers.games.betting.blackjack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlackjackHandTest {
    @Test
    void shouldEvaluateSoftAceWithoutBusting() {
        BlackjackHand hand = new BlackjackHand(100, true);
        hand.add(BlackjackCard.ACE_OF_CLUBS);
        hand.add(BlackjackCard.SIX_OF_HEARTS);

        BlackjackHandValue value = hand.value();

        assertEquals(17, value.total());
        assertTrue(value.soft());
        assertEquals("Soft 17", value.display());
    }

    @Test
    void shouldDowngradeAceWhenElevenWouldBust() {
        BlackjackHand hand = new BlackjackHand(100, true);
        hand.add(BlackjackCard.ACE_OF_CLUBS);
        hand.add(BlackjackCard.SIX_OF_HEARTS);
        hand.add(BlackjackCard.KING_OF_SPADES);

        BlackjackHandValue value = hand.value();

        assertEquals(17, value.total());
        assertFalse(value.soft());
    }

    @Test
    void shouldDetectNaturalOnlyForEligibleTwoCardTwentyOne() {
        BlackjackHand natural = new BlackjackHand(100, true);
        natural.add(BlackjackCard.ACE_OF_CLUBS);
        natural.add(BlackjackCard.KING_OF_SPADES);

        BlackjackHand splitTwentyOne = new BlackjackHand(100, false);
        splitTwentyOne.add(BlackjackCard.ACE_OF_HEARTS);
        splitTwentyOne.add(BlackjackCard.KING_OF_HEARTS);

        assertTrue(natural.isNatural());
        assertFalse(splitTwentyOne.isNatural());
    }

    @Test
    void shouldExposeExpectedEmojiNames() {
        assertEquals("bj_a_c", BlackjackCard.ACE_OF_CLUBS.emojiName());
        assertEquals("bj_10_d", BlackjackCard.TEN_OF_DIAMONDS.emojiName());
        assertEquals("bj_k_s", BlackjackCard.KING_OF_SPADES.emojiName());
        assertEquals("bj_back", BlackjackCard.BACK_EMOJI_NAME);
        assertEquals(52, BlackjackCard.values().length);
    }
}
