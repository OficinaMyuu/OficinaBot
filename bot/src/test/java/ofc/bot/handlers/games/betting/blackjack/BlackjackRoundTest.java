package ofc.bot.handlers.games.betting.blackjack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlackjackRoundTest {
    @Test
    void shouldUseThreeDeckShoe() {
        BlackjackShoe shoe = new BlackjackShoe();

        assertEquals(156, shoe.remaining());
    }

    @Test
    void shouldSettleNaturalBlackjackImmediately() {
        BlackjackRound round = round(
                BlackjackCard.ACE_OF_CLUBS,
                BlackjackCard.NINE_OF_CLUBS,
                BlackjackCard.KING_OF_SPADES,
                BlackjackCard.EIGHT_OF_HEARTS
        );

        assertTrue(round.isSettled());
        assertEquals(BlackjackOutcome.WIN, round.resolvedHands().getFirst().outcome());
    }

    @Test
    void shouldStandDealerOnSoftSeventeen() {
        BlackjackRound round = round(
                BlackjackCard.TEN_OF_CLUBS,
                BlackjackCard.ACE_OF_CLUBS,
                BlackjackCard.SEVEN_OF_SPADES,
                BlackjackCard.SIX_OF_HEARTS,
                BlackjackCard.NINE_OF_DIAMONDS
        );

        round.stand();

        assertTrue(round.isSettled());
        assertEquals(17, round.dealer().value().total());
        assertTrue(round.dealer().value().soft());
        assertEquals(1, round.cardsRemaining());
    }

    @Test
    void shouldHitDealerUntilSeventeen() {
        BlackjackRound round = round(
                BlackjackCard.TEN_OF_CLUBS,
                BlackjackCard.TEN_OF_HEARTS,
                BlackjackCard.SEVEN_OF_SPADES,
                BlackjackCard.TWO_OF_HEARTS,
                BlackjackCard.FIVE_OF_DIAMONDS
        );

        round.stand();

        assertTrue(round.isSettled());
        assertEquals(17, round.dealer().value().total());
        assertEquals(3, round.dealer().cards().size());
    }

    @Test
    void shouldSplitEqualRanksIntoTwoHands() {
        BlackjackRound round = round(
                BlackjackCard.EIGHT_OF_CLUBS,
                BlackjackCard.FIVE_OF_HEARTS,
                BlackjackCard.EIGHT_OF_SPADES,
                BlackjackCard.NINE_OF_CLUBS,
                BlackjackCard.THREE_OF_DIAMONDS,
                BlackjackCard.KING_OF_HEARTS
        );

        assertTrue(round.canSplit());
        round.split();

        assertEquals(2, round.playerHands().size());
        assertEquals(2_000, round.totalStake());
        assertEquals(11, round.playerHands().get(0).value().total());
        assertEquals(18, round.playerHands().get(1).value().total());
        assertFalse(round.playerHands().get(1).isNatural());
    }

    @Test
    void shouldNotSplitDifferentTenValueRanks() {
        BlackjackRound round = round(
                BlackjackCard.JACK_OF_CLUBS,
                BlackjackCard.FIVE_OF_HEARTS,
                BlackjackCard.QUEEN_OF_SPADES,
                BlackjackCard.NINE_OF_CLUBS
        );

        assertFalse(round.canSplit());
    }

    @Test
    void shouldDoubleStakeDrawOnceAndStand() {
        BlackjackRound round = round(
                BlackjackCard.FIVE_OF_CLUBS,
                BlackjackCard.TEN_OF_HEARTS,
                BlackjackCard.SIX_OF_SPADES,
                BlackjackCard.NINE_OF_CLUBS,
                BlackjackCard.KING_OF_DIAMONDS
        );

        round.doubleDown();

        assertTrue(round.isSettled());
        assertEquals(2_000, round.totalStake());
        assertEquals(21, round.playerHands().getFirst().value().total());
        assertEquals(BlackjackOutcome.WIN, round.resolvedHands().getFirst().outcome());
    }

    private BlackjackRound round(BlackjackCard... cards) {
        return new BlackjackRound(1_000, BlackjackShoe.fixed(cards));
    }
}
