package ofc.bot.handlers.economy;

import java.util.Objects;
import java.util.Random;

public class RobberyCalculator {
    public static final double MIN_FAIL_PROBABILITY = 0.2;
    public static final double MAX_FAIL_PROBABILITY = 0.8;
    public static final double MIN_FINE_RATE = 0.2;
    public static final double MAX_FINE_RATE = 0.4;

    private final Random random;

    public RobberyCalculator(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public RobberyAttemptResult roll(long robberNetWorth, int targetWallet) {
        double failProbability = failProbability(robberNetWorth, targetWallet);
        boolean failed = random.nextDouble() < failProbability;
        int amount = failed
                ? fineAmount(robberNetWorth, random.nextDouble(MIN_FINE_RATE, MAX_FINE_RATE))
                : stolenAmount(targetWallet, failProbability);

        return new RobberyAttemptResult(failed, failProbability, amount);
    }

    public static double failProbability(long robberNetWorth, int targetWallet) {
        if (targetWallet <= 0)
            throw new IllegalArgumentException("Target wallet must be positive.");

        long safeNetWorth = Math.max(0, robberNetWorth);
        double probability = (double) safeNetWorth / (targetWallet + (double) safeNetWorth);

        return Math.clamp(probability, MIN_FAIL_PROBABILITY, MAX_FAIL_PROBABILITY);
    }

    public static int stolenAmount(int targetWallet, double failProbability) {
        if (targetWallet <= 0)
            throw new IllegalArgumentException("Target wallet must be positive.");

        double successProbability = 1 - failProbability;
        return Math.max(1, Math.toIntExact(Math.round(Math.ceil(successProbability * targetWallet))));
    }

    public static int fineAmount(long robberNetWorth, double fineRate) {
        if (fineRate < MIN_FINE_RATE || fineRate > MAX_FINE_RATE)
            throw new IllegalArgumentException("Fine rate must be between the configured min and max rates.");

        long safeNetWorth = Math.max(0, robberNetWorth);
        long amount = Math.round(safeNetWorth * fineRate);
        return Math.toIntExact(Math.clamp(amount, 0, Integer.MAX_VALUE));
    }

    public record RobberyAttemptResult(boolean failed, double failProbability, int amount) {
        public double successProbability() {
            return 1 - failProbability;
        }
    }
}
