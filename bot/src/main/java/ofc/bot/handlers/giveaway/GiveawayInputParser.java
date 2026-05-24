package ofc.bot.handlers.giveaway;

import ofc.bot.util.time.OficinaDuration;

public final class GiveawayInputParser {
    private GiveawayInputParser() {}

    public static long parseDurationSeconds(String input) {
        try {
            return OficinaDuration.ofPattern(input).getSeconds();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public static long parsePositiveMoney(String input) {
        if (input == null || input.equalsIgnoreCase("all")) {
            return -1;
        }

        String normalized = input.strip().toLowerCase()
                .replaceFirst("k", "000")
                .replaceFirst("kk", "000000")
                .replaceFirst("m", "000000");

        try {
            long amount = Long.parseLong(normalized);
            return amount > 0 && amount <= Integer.MAX_VALUE ? amount : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
