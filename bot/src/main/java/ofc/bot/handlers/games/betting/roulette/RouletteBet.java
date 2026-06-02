package ofc.bot.handlers.games.betting.roulette;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public final class RouletteBet {
    private static final List<String> SUGGESTIONS = List.of(
            "red", "black", "even", "odd", "1-18", "19-36",
            "1st", "2nd", "3rd", "1-12", "13-24", "25-36",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "10", "11", "12", "13", "14", "15", "16", "17", "18",
            "19", "20", "21", "22", "23", "24", "25", "26", "27",
            "28", "29", "30", "31", "32", "33", "34", "35", "36"
    );

    private final String canonicalName;
    private final int multiplier;
    private final Matcher matcher;

    private RouletteBet(String canonicalName, int multiplier, Matcher matcher) {
        this.canonicalName = canonicalName;
        this.multiplier = multiplier;
        this.matcher = matcher;
    }

    public static Optional<RouletteBet> parse(String raw) {
        if (raw == null) return Optional.empty();

        String value = normalize(raw);
        return switch (value) {
            case "red" -> Optional.of(color("red", RouletteColor.RED));
            case "black" -> Optional.of(color("black", RouletteColor.BLACK));
            case "even" -> Optional.of(new RouletteBet("even", 2, number -> number > 0 && number % 2 == 0));
            case "odd" -> Optional.of(new RouletteBet("odd", 2, number -> number > 0 && number % 2 != 0));
            case "1-18" -> Optional.of(range("1-18", 1, 18, 2));
            case "19-36" -> Optional.of(range("19-36", 19, 36, 2));
            case "1st", "first" -> Optional.of(column("1st", 1));
            case "2nd", "second" -> Optional.of(column("2nd", 2));
            case "3rd", "third" -> Optional.of(column("3rd", 3));
            case "1-12" -> Optional.of(range("1-12", 1, 12, 3));
            case "13-24" -> Optional.of(range("13-24", 13, 24, 3));
            case "25-36" -> Optional.of(range("25-36", 25, 36, 3));
            default -> parseNumber(value);
        };
    }

    public static List<String> suggestions(String search) {
        String normalized = normalize(search);
        Stream<String> stream = SUGGESTIONS.stream();

        if (!normalized.isEmpty()) {
            stream = stream.filter(s -> s.startsWith(normalized));
        }

        return stream.limit(OptionData.MAX_CHOICES).toList();
    }

    public boolean wins(RouletteSpin spin) {
        return matcher.matches(spin.number());
    }

    public int payoutFor(int amount) {
        return Math.multiplyExact(amount, multiplier);
    }

    public String canonicalName() {
        return canonicalName;
    }

    public int multiplier() {
        return multiplier;
    }

    private static RouletteBet color(String name, RouletteColor color) {
        return new RouletteBet(name, 2, number -> RouletteColor.fromNumber(number) == color);
    }

    private static RouletteBet range(String name, int start, int end, int multiplier) {
        return new RouletteBet(name, multiplier, number -> number >= start && number <= end);
    }

    private static RouletteBet column(String name, int column) {
        return new RouletteBet(name, 3, number -> number > 0 && (number - column) % 3 == 0);
    }

    private static Optional<RouletteBet> parseNumber(String value) {
        try {
            int number = Integer.parseInt(value);
            if (number < RouletteSpin.MIN_NUMBER || number > RouletteSpin.MAX_NUMBER) {
                return Optional.empty();
            }
            return Optional.of(new RouletteBet(String.valueOf(number), 36, spinNumber -> spinNumber == number));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static String normalize(String raw) {
        return raw.strip().toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface Matcher {
        boolean matches(int number);
    }
}
