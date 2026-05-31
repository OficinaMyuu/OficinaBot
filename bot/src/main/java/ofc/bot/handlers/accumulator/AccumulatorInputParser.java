package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AccumulatorInputParser {
    private static final Pattern USER_MENTION = Pattern.compile("<@!?(\\d+)>");
    private static final Pattern SNOWFLAKE = Pattern.compile("\\d{1,20}");

    private AccumulatorInputParser() {}

    public static Result parse(AccumulatorPrizeType type, User user, String entries, Integer defaultAmount) {
        List<Entry> parsed = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (user != null) {
            addEntry(parsed, errors, "user", type, user.getId(), defaultAmount);
        }

        if (entries != null && !entries.isBlank()) {
            String[] lines = entries.strip().split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].strip();
                if (line.isBlank()) {
                    continue;
                }
                addEntry(parsed, errors, "line " + (i + 1), type, line, defaultAmount);
            }
        }

        if (parsed.isEmpty() && errors.isEmpty()) {
            errors.add("Provide at least one user or one bulk entry.");
        }

        return new Result(parsed, errors);
    }

    private static void addEntry(
            List<Entry> parsed,
            List<String> errors,
            String source,
            AccumulatorPrizeType type,
            String raw,
            Integer defaultAmount
    ) {
        String[] tokens = raw.split("[\\s,;]+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            errors.add(source + ": missing user id.");
            return;
        }

        Long userId = parseUserId(tokens[0]);
        if (userId == null) {
            errors.add(source + ": invalid user id `" + tokens[0] + "`.");
            return;
        }

        Integer amount = defaultAmount;
        if (type == AccumulatorPrizeType.MONEY) {
            if (tokens.length > 2) {
                errors.add(source + ": use `<user> <amount>` for money entries.");
                return;
            }

            if (tokens.length == 2) {
                amount = parseAmount(tokens[1]);
                if (amount == null) {
                    errors.add(source + ": invalid amount `" + tokens[1] + "`.");
                    return;
                }
            }

            if (!isValidAmount(amount)) {
                errors.add(source + ": amount must be between 1 and " + AccumulatorPrize.MAX_AMOUNT + ".");
                return;
            }
        } else if (tokens.length > 1) {
            errors.add(source + ": color role entries only accept a user id.");
            return;
        }

        parsed.add(new Entry(userId, amount));
    }

    private static Long parseUserId(String raw) {
        Matcher mention = USER_MENTION.matcher(raw);
        String value = mention.matches() ? mention.group(1) : raw;

        if (!SNOWFLAKE.matcher(value).matches()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseAmount(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isValidAmount(Integer amount) {
        return amount != null && amount > 0 && amount <= AccumulatorPrize.MAX_AMOUNT;
    }

    public record Entry(long userId, Integer amount) {}

    public record Result(List<Entry> entries, List<String> errors) {
        public boolean isOk() {
            return errors.isEmpty();
        }
    }
}
