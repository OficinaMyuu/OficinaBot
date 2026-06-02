package ofc.bot.handlers.games.betting.roulette;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

public record RouletteSpin(int number, RouletteColor color) {
    public static final int MIN_NUMBER = 0;
    public static final int MAX_NUMBER = 36;

    public RouletteSpin {
        if (number < MIN_NUMBER || number > MAX_NUMBER) {
            throw new IllegalArgumentException("Roulette number must be between 0 and 36, provided: " + number);
        }
        if (color == null) {
            throw new IllegalArgumentException("Roulette color cannot be null");
        }
    }

    public static RouletteSpin of(int number) {
        return new RouletteSpin(number, RouletteColor.fromNumber(number));
    }

    public static RouletteSpin random() {
        return random(() -> ThreadLocalRandom.current().nextInt(MAX_NUMBER + 1));
    }

    public static RouletteSpin random(IntSupplier supplier) {
        return of(supplier.getAsInt());
    }
}
