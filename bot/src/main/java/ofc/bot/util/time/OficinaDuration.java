package ofc.bot.util.time;

import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable duration class that supports years, months, days,
 * hours, minutes, seconds, and milliseconds.
 * <p>
 * Internally, the duration is stored as a total number of milliseconds.
 * <p>
 * Note: This implementation uses fixed conversion factors:
 * <ul>
 *   <li>1 year = 365 days</li>
 *   <li>1 month = 30 days</li>
 *   <li>1 day = 24 hours</li>
 *   <li>1 hour = 60 minutes</li>
 *   <li>1 minute = 60 seconds</li>
 *   <li>1 second = 1000 milliseconds</li>
 * </ul>
 * <p>
 * For each unit (except years) there is a pair of getters: one that returns the total
 * count in that unit and one that returns the "part" (the normalized remainder when subtracting
 * higher units). For example, if the total duration amounts to 2 years and 1 month, then
 * {@link #getYears} returns {@code 2}, {@link #getMonths} returns {@code 25} and
 * {@link #getMonthsPart} returns {@code 1}.
 * <p>
 * Days are normalized to the range 0–29.
 */
public final class OficinaDuration {
    public static final OficinaDuration ZERO = new OficinaDuration(0);

    // Conversion factors (using approximate values for years and months)
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    private static final long MILLIS_PER_HOUR   = 60 * MILLIS_PER_MINUTE;
    private static final long MILLIS_PER_DAY    = 24 * MILLIS_PER_HOUR;
    private static final long MILLIS_PER_MONTH  = 30 * MILLIS_PER_DAY;
    private static final long MILLIS_PER_YEAR   = 365 * MILLIS_PER_DAY;

    // Internal storage of the duration as total milliseconds.
    private final long millis;
    // Cached breakdown array containing:
    // [0] years,
    // [1] months (0 to 11)
    // [2] days (0 to 29)
    // [3] hours (0 to 23)
    // [4] minutes (0 to 59)
    // [5] seconds (0 to 59)
    // [6] milliseconds (0 to 999)
    private final long[] breakdowns;

    // The pattern for parsing strings
    // Supported tokens:
    //   y  → years
    //   mo → months
    //   d  → days
    //   h  → hours
    //   m  → minutes (note: "mo" is used for months)
    //   s  → seconds
    //   ms → milliseconds
    private static final Pattern DURATION_PATTERN;

    // Private constructor taking a total milliseconds value.
    private OficinaDuration(long millis) {
        Checks.notNegative(millis, "Millis");
        this.millis = millis;
        this.breakdowns = breakdown();
    }

    /**
     * Parses a duration string into an OficinaDuration.
     * <p>
     * The string format is a sequence of number+unit tokens. Supported units are:
     * <ul>
     *   <li>{@code y}  - years (assumed 365 days)</li>
     *   <li>{@code mo} - months (assumed 30 days)</li>
     *   <li>{@code d}  - days</li>
     *   <li>{@code h}  - hours</li>
     *   <li>{@code m}  - minutes (use "mo" for months)</li>
     *   <li>{@code s}  - seconds</li>
     *   <li>{@code ms} - milliseconds</li>
     * </ul>
     * For example: {@code "1y 2mo 3d 4h 5m 6s 7ms"}.
     *
     * @param input the duration string.
     * @return the corresponding OficinaDuration.
     * @throws IllegalArgumentException if the input doesn't match the expected pattern.
     */
    public static OficinaDuration ofPattern(@NotNull String input) {
        Checks.notNull(input, "Pattern");
        if (input.isBlank()) return ZERO;

        Matcher matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid duration pattern: " + input);
        }

        long totalMillis = 0;
        // Group order:
        // 1 - years
        // 2 - months
        // 3 - days
        // 4 - hours
        // 5 - minutes
        // 6 - seconds
        // 7 - milliseconds
        totalMillis += parseGroup(matcher.group(1)) * MILLIS_PER_YEAR;
        totalMillis += parseGroup(matcher.group(2)) * MILLIS_PER_MONTH;
        totalMillis += parseGroup(matcher.group(3)) * MILLIS_PER_DAY;
        totalMillis += parseGroup(matcher.group(4)) * MILLIS_PER_HOUR;
        totalMillis += parseGroup(matcher.group(5)) * MILLIS_PER_MINUTE;
        totalMillis += parseGroup(matcher.group(6)) * MILLIS_PER_SECOND;
        totalMillis += parseGroup(matcher.group(7));

        return new OficinaDuration(totalMillis);
    }

    private static long parseGroup(String group) {
        return (group == null || group.isEmpty()) ? 0L : Long.parseLong(group);
    }

    // -------------------- Arithmetic Methods --------------------

    public OficinaDuration plusYears(long years) {
        return new OficinaDuration(this.millis + years * MILLIS_PER_YEAR);
    }

    public OficinaDuration plusMonths(long months) {
        return new OficinaDuration(this.millis + months * MILLIS_PER_MONTH);
    }

    public OficinaDuration plusDays(long days) {
        return new OficinaDuration(this.millis + days * MILLIS_PER_DAY);
    }

    public OficinaDuration plusHours(long hours) {
        return new OficinaDuration(this.millis + hours * MILLIS_PER_HOUR);
    }

    public OficinaDuration plusMinutes(long minutes) {
        return new OficinaDuration(this.millis + minutes * MILLIS_PER_MINUTE);
    }

    public OficinaDuration plusSeconds(long seconds) {
        return new OficinaDuration(this.millis + seconds * MILLIS_PER_SECOND);
    }

    public OficinaDuration plusMillis(long millis) {
        return new OficinaDuration(this.millis + millis);
    }

    // -------------------- Breakdown Helper --------------------
    /**
     * Breaks down the total milliseconds into its normalized components.
     * The result is an array of seven values:
     * <ol>
     *   <li>years</li>
     *   <li>months (0 to 11)</li>
     *   <li>days (0 to 29)</li>
     *   <li>hours (0 to 23)</li>
     *   <li>minutes (0 to 59)</li>
     *   <li>seconds (0 to 59)</li>
     *   <li>milliseconds (0 to 999)</li>
     * </ol>
     */
    private long[] breakdown() {
        long rem = millis;
        long years = rem / MILLIS_PER_YEAR;
        rem %= MILLIS_PER_YEAR;
        long months = rem / MILLIS_PER_MONTH;
        // Normalize months so that 12 months become an extra year.
        if (months >= 12) {
            years += months / 12;
            months = 0;
        }
        rem %= MILLIS_PER_MONTH;
        long days = rem / MILLIS_PER_DAY;
        // Normalize days so that 30 days become an extra month.
        rem %= MILLIS_PER_DAY;
        long hours = rem / MILLIS_PER_HOUR;
        rem %= MILLIS_PER_HOUR;
        long minutes = rem / MILLIS_PER_MINUTE;
        rem %= MILLIS_PER_MINUTE;
        long seconds = rem / MILLIS_PER_SECOND;
        rem %= MILLIS_PER_SECOND;
        long millisPart = rem;
        return new long[]{years, months, days, hours, minutes, seconds, millisPart};
    }

    // -------------------- Getters --------------------

    /**
     * Returns the number of complete years in this duration.
     */
    public long getYears() {
        return breakdowns[0];
    }

    /**
     * Returns the total number of months in this duration.
     * <p>
     * This is computed by converting all the years into months and adding the months part.
     * For example, if {@link #getYears} returns {@code 2} and {@link #getMonthsPart} returns {@code 1},
     * then this method returns {@code 25}.
     */
    public long getMonths() {
        return getYears() * 12 + breakdowns[1];
    }

    /**
     * Returns the "months part" of the duration (0–11) after extracting full years.
     */
    public long getMonthsPart() {
        return breakdowns[1];
    }

    /**
     * Returns the total number of days in this duration.
     */
    public long getDays() {
        return millis / MILLIS_PER_DAY;
    }

    /**
     * Returns the "days part" of the duration (0–29) after extracting months.
     */
    public long getDaysPart() {
        return breakdowns[2];
    }

    /**
     * Returns the total number of hours in this duration.
     */
    public long getHours() {
        return millis / MILLIS_PER_HOUR;
    }

    /**
     * Returns the "hours part" of the duration (0–23) after extracting days.
     */
    public long getHoursPart() {
        return breakdowns[3];
    }

    /**
     * Returns the total number of minutes in this duration.
     */
    public long getMinutes() {
        return millis / MILLIS_PER_MINUTE;
    }

    /**
     * Returns the "minutes part" (0–59) after extracting hours.
     */
    public long getMinutesPart() {
        return breakdowns[4];
    }

    /**
     * Returns the total number of seconds in this duration.
     */
    public long getSeconds() {
        return millis / MILLIS_PER_SECOND;
    }

    /**
     * Returns the "seconds part" (0–59) after extracting minutes.
     */
    public long getSecondsPart() {
        return breakdowns[5];
    }

    /**
     * Returns the total number of milliseconds in this duration.
     */
    public long getMillis() {
        return millis;
    }

    /**
     * Returns the "milliseconds part" (0–999) after extracting seconds.
     */
    public long getMillisPart() {
        return breakdowns[6];
    }

    /**
     * Converts this OficinaDuration to a {@link Duration}.
     * <p>
     * <b>Note</b>: This conversion uses the total milliseconds so any conceptual
     * differences regarding months or years are lost.
     *
     * @return the equivalent {@link Duration}.
     */
    public Duration toDuration() {
        return Duration.ofMillis(millis);
    }

    /**
     * Checks whether the provided {@code amount} is greater than
     * this duration.
     *
     * @param amount the amount of time.
     * @param unit the unit of time used in the {@code amount} parameter.
     * @return {@code true} if {@code amount} is greater than the amount of time in this instance,
     *         {@code false} otherwise.
     */
    public boolean isGreater(long amount, @NotNull TimeUnit unit) {
        Checks.notNull(unit, "TimeUnit");
        return unit.toMillis(amount) > this.millis;
    }

    /**
     * Checks whether the provided {@link Duration} is longer than
     * this duration.
     *
     * @param duration the duration to be compared.
     * @return {@code true} if the provided duration is longer than the amount of time in this instance,
     *         {@code false} otherwise.
     */
    public boolean isGreater(@NotNull Duration duration) {
        Checks.notNull(duration, "Duration");
        return isGreater(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Checks whether the amount of milliseconds is greater than the total
     * in this duration.
     *
     * @param millis the amount of milliseconds.
     * @return {@code true} if {@code millis} is greater than the amount of time in this instance,
     *         {@code false} otherwise.
     */
    public boolean isGreater(long millis) {
        return isGreater(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Checks whether the provided {@code amount} is less than
     * this duration.
     *
     * @param amount the amount of time.
     * @param unit the unit of time used in the {@code amount} parameter.
     * @return {@code true} if {@code amount} is less than the amount of time in this instance,
     *         {@code false} otherwise.
     */
    public boolean isLess(long amount, @NotNull TimeUnit unit) {
        Checks.notNull(unit, "TimeUnit");
        return unit.toMillis(amount) < this.millis;
    }

    /**
     * Checks whether the provided {@link Duration} is shorter than
     * this duration.
     *
     * @param duration the duration to be compared.
     * @return {@code true} if the provided duration is shorter than the amount of time in this instance,
     *         {@code false} otherwise.
     */
    public boolean isLess(@NotNull Duration duration) {
        Checks.notNull(duration, "Duration");
        return isLess(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Checks whether the amount of milliseconds is less than the total
     * in this duration.
     *
     * @param millis the amount of milliseconds.
     * @return {@code true} if {@code millis} is less than the amount of time in this instance,
     *         {@code false} otherwise.
     */
    public boolean isLess(long millis) {
        return isLess(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        long[] parts = breakdown();
        return String.format("%d years, %d months, %d days, %d hours, %d minutes, %d seconds, and %d milliseconds",
                parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
    }

    static {
        DURATION_PATTERN = Pattern.compile(
                "^\\s*(?:(\\d+)y)?\\s*(?:(\\d+)mo)?\\s*(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)(?!o)m)?\\s*(?:(\\d+)s)?\\s*(?:(\\d+)ms)?\\s*$",
                Pattern.CASE_INSENSITIVE
        );
    }
}