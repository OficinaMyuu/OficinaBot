package ofc.bot.handlers.games.betting.roulette;

import java.util.Set;

public enum RouletteColor {
    RED,
    BLACK,
    GREEN;

    private static final Set<Integer> RED_NUMBERS = Set.of(
            1, 3, 5, 7, 9, 12, 14, 16, 18,
            19, 21, 23, 25, 27, 30, 32, 34, 36
    );

    public static RouletteColor fromNumber(int number) {
        if (number == 0) return GREEN;
        return RED_NUMBERS.contains(number) ? RED : BLACK;
    }

    public String displayName() {
        return name().toLowerCase();
    }
}
