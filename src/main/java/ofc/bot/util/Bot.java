package ofc.bot.util;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.Main;
import ofc.bot.domain.entity.enums.Gender;
import ofc.bot.internal.data.BotProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class Bot {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
    private static final Locale LOCALE = Locale.of("pt", "BR");
    private static final String KEY_FEMALE_ID = "roles.genders.female.id";
    private static final String KEY_MALE_ID = "roles.genders.male.id";

    private static final int THOUSAND = 1_000;
    private static final int MILLION = 1_000 * THOUSAND;
    private static final int BILLION = 1_000 * MILLION;
    
    private Bot() {}

    public static Locale defaultLocale() {
        return LOCALE;
    }

    @NotNull
    public static Gender findGender(@NotNull Member member) {
        Checks.notNull(member, "Member");
        List<Long> roles = member.getRoles().stream().map(Role::getIdLong).toList();
        long maleId = getSafe(KEY_MALE_ID, Long::parseLong);
        long femId = getSafe(KEY_FEMALE_ID, Long::parseLong);

        if (roles.contains(maleId)) {
            return Gender.MALE;
        }

        if (roles.contains(femId)) {
            return Gender.FEMALE;
        }
        return Gender.UNKNOWN;
    }

    public static <T, A extends Annotation> T getSafeAnnotationValue(
            @NotNull Object obj, @NotNull Class<A> annotation, @NotNull Function<A, T> mapper) {
        return getSafeAnnotationValue(obj.getClass(), annotation, mapper);
    }

    public static <T, A extends Annotation> T getSafeAnnotationValue(
            @NotNull Class<?> clazz, @NotNull Class<A> annotation, @NotNull Function<A, T> mapper) {
        T val = getAnnotationValue(clazz, annotation, mapper, null);
        if (val == null) {
            throw new IllegalStateException("The class " + clazz.getName() + " has no annotation " +
                    annotation.getName() + " or mapped to null");
        }
        return val;
    }

    public static <T, A extends Annotation> T getAnnotationValue(
            @NotNull Object obj, @NotNull Class<A> annotation, @NotNull Function<A, T> mapper, T fallback) {
        return getAnnotationValue(obj.getClass(), annotation, mapper, fallback);
    }

    public static <T, A extends Annotation> T getAnnotationValue(
            @NotNull Class<?> clazz, @NotNull Class<A> annotation, @NotNull Function<A, T> mapper, T fallback) {
        A ann = clazz.getDeclaredAnnotation(annotation);
        if (ann == null) return fallback;

        return mapper.apply(ann);
    }

    @Nullable
    public static String get(String key) {
        try {
            return BotProperties.find(key);
        } catch (DataAccessException e) {
            LOGGER.error("Could not fetch properties for key ({})", key, e);
            return null;
        }
    }

    public static <T> T get(String key, Function<String, T> mapper) {
        String val = get(key);
        return val == null ? null : mapper.apply(val);
    }

    public static String getSafe(String key) {
        String value = get(key);
        if (value == null)
            throw new NoSuchElementException("Found no values for key " + key);

        return value;
    }

    public static <T> T getSafe(String key, Function<String, T> mapper) {
        return mapper.apply(getSafe(key));
    }

    public static List<GatewayIntent> getIntents() {
        return List.of(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_EXPRESSIONS,
                GatewayIntent.GUILD_MODERATION,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.AUTO_MODERATION_EXECUTION,
                GatewayIntent.SCHEDULED_EVENTS,
                GatewayIntent.DIRECT_MESSAGES
        );
    }

    public static boolean isZero(Number val) {
        return val == null || val.doubleValue() == 0;
    }

    public static boolean isNegative(Number val) {
        return val == null || val.doubleValue() < 0;
    }

    public static boolean chance(byte chance) {
        if (chance <= 0) return false;
        if (chance >= 100) return true;

        int random = ThreadLocalRandom.current().nextInt(100);
        return random < chance;
    }

    public static boolean chance(double chance) {
        if (chance <= 0) return false;
        if (chance >= 100) return true;

        double random = ThreadLocalRandom.current().nextDouble(100.0);
        return random < chance;
    }

    private static final double MIN_CHANCE = 1.0;
    private static final double MAX_CHANCE = 60.0;
    private static final double DAY_SECONDS = 86400.0;
    public static double getMediocreChance() {
        LocalTime time = LocalTime.now();
        int seconds = time.toSecondOfDay();
        double chanceSpan = MAX_CHANCE - MIN_CHANCE;
        double dayProportion = (double) seconds / DAY_SECONDS;

        return MIN_CHANCE + (dayProportion * chanceSpan);
    }

    /**
     * Checks whether a number is positive.
     * {@code 0} is considered a positive value.
     *
     * @param val The value to be checked.
     * @return {@code true} if the provided value is positive, {@code false} otherwise.
     */
    public static boolean isPositive(Number val) {
        return !isNegative(val);
    }

    public static <T> T ifNull(T obj, T fallback) {
        return obj == null ? fallback : obj;
    }

    public static int parseAmount(String input, int balance) {
        if (input.equalsIgnoreCase("all"))
            return balance;

        String inputlc = input.toLowerCase();

        // By using replaceFirst() we ensure that users will not make
        // mistakes such as sending an obscene amount of money,
        // by "unwantingly" providing "5mm" and sending, yknow... "5000000000000"
        String parse = inputlc
                .replaceFirst("k", "000")
                .replaceFirst("kk", "000000")
                .replaceFirst("m", "000000");

        try {
            return Integer.parseInt(parse);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String limitStr(String str, int limit) {
        if (limit <= 3)
            throw new IllegalArgumentException("Limit cannot be less than or equal to 3");

        if (str.length() <= limit) return str;

        String newStr = str.substring(0, limit - 3);
        return newStr + "...";
    }

    public static int calcMaxPages(int total, int pageSize) {
        int maxPages = (int) Math.ceil((double) total / pageSize);
        return Math.max(maxPages, 1);
    }

    public static void delete(Message message) {
        message.delete()
                .queue(null, new ErrorHandler()
                        .ignore(ErrorResponse.UNKNOWN_MESSAGE));
    }

    public static String upperFirst(String str) {
        if (str == null || str.isEmpty()) return str;

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static CacheRestAction<User> fetchUser(long userId) {
        return Main.getApi().retrieveUserById(userId);
    }

    public static String parseDuration(Duration duration) {
        return parsePeriod(duration.getSeconds());
    }

    public static String parsePeriod(long seconds) {
        if (seconds <= 0)
            return "0s";
        
        StringBuilder builder = new StringBuilder();
        Duration duration = Duration.ofSeconds(seconds);

        long day = duration.toDaysPart();
        int hrs = duration.toHoursPart();
        int min = duration.toMinutesPart();
        int sec = duration.toSecondsPart();

        if (day != 0) builder.append(String.format("%02dd, ", day));
        if (hrs != 0) builder.append(String.format("%02dh, ", hrs));
        if (min != 0) builder.append(String.format("%02dm, ", min));
        if (sec != 0) builder.append(String.format("%02ds, ", sec));

        String result = builder.toString().stripTrailing();
        return result.substring(0, result.length() - 1);
    }

    public static long unixNow() {
        return Instant.now().getEpochSecond();
    }

    public static long unixMidnightNow() {
        return LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();
    }

    public static long distance(long a, long b) {
        return Math.abs(a - b);
    }

    public static boolean writeToFile(String content, File file) {
        try (
                OutputStream out = Files.newOutputStream(Path.of(file.getAbsolutePath()));
                Writer writer = new OutputStreamWriter(out)
        ) {
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            LOGGER.error("Could not write to file {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    public static String fmtNum(long value) {
        if (value == 0) return "0";

        if (value > -10 && value < 10) return String.format("%02d", value);

        NumberFormat currency = NumberFormat.getNumberInstance(LOCALE);

        return currency.format(value);
    }

    public static String humanizeNum(long value) {
        double num = (double) value;
        if (num >= BILLION) {
            return String.format("%.2fB", num / BILLION);
        } else if (num >= MILLION) {
            return String.format("%.2fM", num / MILLION);
        } else if (num >= THOUSAND) {
            return String.format("%.2fK", num / THOUSAND);
        } else {
            return String.format("%d", value);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... values) {
        if (values.length == 0) return Map.of();

        if (isOdd(values.length))
            throw new IllegalArgumentException("Odd amount of arguments");

        Map<Object, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            Object key = values[i];
            Object value = values[i + 1];
            map.put(key, value);
        }
        return (Map<K, V>) Collections.unmodifiableMap(map);
    }

    public static boolean isOdd(long value) {
        return value % 2 != 0;
    }

    public static String fmtColorHex(int color) {
        return String.format("#%06X", color);
    }

    public static int toRGB(Color color) {
        return color.getRGB() & 0x00FFFFFF;
    }

    public static String fmtMoney(long value) {
        return '$' + fmtNum(value);
    }

    public static <T> String format(final List<T> values, final Function<T, String> format) {
        if (values.isEmpty()) return "";

        StringBuilder builder = new StringBuilder();

        for (T value : values) {
            builder.append(format.apply(value));
        }

        return builder.toString().strip();
    }

    /**
     * Returns the {@code int} RGB color of this HEX string.
     *
     * @param hex the HEX color value.
     * @return the RGB value of the color, or {@code -1} if the argument is not of length {@code 6}.
     * @throws NumberFormatException if the {@code String} does not contain a parsable {@code int}.
     */
    public static int hexToRgb(String hex) {
        if (hex.length() != 6) return -1;

        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);

        return (red << 16) | (green << 8) | blue;
    }

    public static final class Colors {
        public static final Color DISCORD = new Color(88, 101, 242);
        public static final Color TWITCH = new Color(144, 70, 254);
        public static final Color DEFAULT = new Color(170, 67, 254);
    }

    public static final class Emojis {
        public static final Emoji GRAY_ARROW_LEFT  = Emoji.fromFormatted("<:arrowleft:1331425997890785290>");
        public static final Emoji GRAY_ARROW_RIGHT = Emoji.fromFormatted("<:arrowright:1331425991205191730>");
        public static final Emoji INV = Emoji.fromFormatted("<:inv:1347081576298844180>");
        public static final Emoji LOADING = Emoji.fromFormatted("<a:loading:1293036166387601469>");
    }
}