package ofc.bot.handlers.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RobberyCalculatorTest {
    @Test
    void shouldCalculateUnclampedFailProbabilityFromRobberNetWorthAndTargetWallet() {
        double probability = RobberyCalculator.failProbability(500, 500);

        assertEquals(0.5, probability);
    }

    @Test
    void shouldClampFailProbabilityToConfiguredBounds() {
        assertEquals(0.2, RobberyCalculator.failProbability(1, 1_000));
        assertEquals(0.8, RobberyCalculator.failProbability(1_000, 1));
    }

    @Test
    void shouldTreatNegativeNetWorthAsZeroForProbability() {
        double probability = RobberyCalculator.failProbability(-500, 1_000);

        assertEquals(0.2, probability);
    }

    @Test
    void shouldCalculateStolenAmountFromSuccessProbabilityAndTargetWallet() {
        assertEquals(500, RobberyCalculator.stolenAmount(1_000, 0.5));
        assertEquals(800, RobberyCalculator.stolenAmount(1_000, 0.2));
        assertEquals(200, RobberyCalculator.stolenAmount(1_000, 0.8));
    }

    @Test
    void shouldRoundStolenAmountUpAndStealAtLeastOneCoin() {
        assertEquals(334, RobberyCalculator.stolenAmount(500, 1.0 / 3.0));
        assertEquals(1, RobberyCalculator.stolenAmount(1, 0.8));
    }

    @Test
    void shouldCalculateCrimeStyleFineFromRobberNetWorth() {
        assertEquals(200, RobberyCalculator.fineAmount(1_000, 0.2));
        assertEquals(400, RobberyCalculator.fineAmount(1_000, 0.4));
    }

    @Test
    void shouldNeverRewardUsersWithNegativeNetWorthOnFailure() {
        assertEquals(0, RobberyCalculator.fineAmount(-1_000, 0.4));
    }

    @Test
    void shouldRejectInvalidWalletsAndFineRates() {
        assertThrows(IllegalArgumentException.class, () -> RobberyCalculator.failProbability(100, 0));
        assertThrows(IllegalArgumentException.class, () -> RobberyCalculator.stolenAmount(0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> RobberyCalculator.fineAmount(100, 0.1));
        assertThrows(IllegalArgumentException.class, () -> RobberyCalculator.fineAmount(100, 0.5));
    }
}
