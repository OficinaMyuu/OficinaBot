package ofc.bot.domain.entity;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.entity.enums.ReminderType;
import ofc.bot.domain.tables.RemindersTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.List;

public class Reminder extends OficinaRecord<Reminder> {
    private static final RemindersTable REMINDERS = RemindersTable.REMINDERS;

    public static final int MAX_PER_USER = 10;
    public static final int MIN_DURATION = 2 * 60; // 2 minutes in seconds

    private static final Logger LOGGER = LoggerFactory.getLogger(Reminder.class);
    private static final List<ChannelType> ALLOWED_CHANNEL_TYPES = List.of(ChannelType.TEXT, ChannelType.PRIVATE);

    public Reminder() {
        super(REMINDERS);
    }

    /**
     * This is the default constructor of a reminder created by a user.
     * <p>
     * Though you can use it directly, it is highly discouraged, as you can make mistakes.
     * This way, you should use the static utility methods to instantiate a new {@link Reminder} object.
     */
    public Reminder(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message,
                    @NotNull ReminderType type, @Nullable Long reminderValue, @Nullable String expression,
                    int times, long timeCreated, long timeUpdated) {
        this();
        Checks.notNull(message, "Message");
        Checks.notNull(chanType, "Channel Type");
        Checks.notNull(type, "Type");
        Checks.notNegative(times, "Execution Times");
        checkChannelType(chanType);

        if (reminderValue != null && reminderValue <= 0)
            throw new IllegalArgumentException("Reminder value must be greater than zero, provided: " + reminderValue);

        if (!type.isRepeatable() && times > 1)
            throw new IllegalArgumentException("Type " + type + " is not repeatable, but an interval was provided: " + times);

        if (type.isExpression() && expression == null)
            throw new IllegalArgumentException("Type " + type + " was provided with no expression");

        if (!type.isExpression() && Bot.isZero(reminderValue))
            throw new IllegalArgumentException("Type " + type + " was provided with no duration/timestamp provided");

        if (expression != null && Bot.isPositive(reminderValue))
            LOGGER.warn("Received both an expression and a remind_at value. As this is probably a mistake, " +
                    "{} will be ignored based on the ReminderType ({}) provided.",
                    type.isExpression() ? "reminderValue" : "expression", type);

        set(REMINDERS.USER_ID, userId);
        set(REMINDERS.CHANNEL_ID, chanId);
        set(REMINDERS.CHANNEL_TYPE, chanType.name());
        set(REMINDERS.MESSAGE, message);
        set(REMINDERS.TYPE, type.name());
        set(REMINDERS.TRIGGER_TIMES, times);
        set(REMINDERS.TRIGGERS_LEFT, times);
        set(REMINDERS.CREATED_AT, timeCreated);
        set(REMINDERS.UPDATED_AT, timeUpdated);

        // Avoid sending dumb values to the database
        set(REMINDERS.REMINDER_VALUE, type.isExpression() ? 0 : reminderValue);
        set(REMINDERS.EXPRESSION, type.isExpression() ? expression : null);
    }

    public static List<ChannelType> getAllowedChannelTypes() {
        return ALLOWED_CHANNEL_TYPES;
    }

    public static boolean isChannelAllowed(ChannelType type) {
        return ALLOWED_CHANNEL_TYPES.contains(type);
    }

    /* ---------- AT ---------- */
    public static Reminder ofAt(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message, @NotNull ZonedDateTime moment, long timeCreated, long timeUpdated) {
        long secs = moment.toInstant().getEpochSecond();
        return new Reminder(userId, chanId, chanType, message, ReminderType.AT, secs, null, 0, timeCreated, timeUpdated);
    }

    public static Reminder ofAt(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message, @NotNull ZonedDateTime moment) {
        long now = Bot.unixNow();
        return ofAt(userId, chanId, chanType, message, moment, now, now);
    }

    /* ---------- PERIOD ---------- */
    public static Reminder ofPeriod(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message, long periodSecs,
                                    int times, long timeCreated, long timeUpdated) {
        return new Reminder(userId, chanId, chanType, message, ReminderType.PERIOD, periodSecs, null, times, timeCreated, timeUpdated);
    }

    public static Reminder ofPeriod(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message, long periodSecs, int times) {
        long now = Bot.unixNow();
        return ofPeriod(userId, chanId, chanType, message, periodSecs, times, now, now);
    }

    public static Reminder ofPeriod(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message, long periodSecs) {
        return ofPeriod(userId, chanId, chanType, message, periodSecs, 0);
    }

    /* ---------- CRON ---------- */
    public static Reminder ofCron(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message, @NotNull String expression,
                                  long timeCreated, long timeUpdated) {
        return new Reminder(userId, chanId, chanType, message, ReminderType.CRON, null, expression, 0, timeCreated, timeUpdated);
    }

    public static Reminder ofCron(long userId, long chanId, @NotNull ChannelType chanType, @NotNull String message, @NotNull String expression) {
        long now = Bot.unixNow();
        return ofCron(userId, chanId, chanType, message, expression, now, now);
    }

    public int getId() {
        return get(REMINDERS.ID);
    }

    public long getUserId() {
        return get(REMINDERS.USER_ID);
    }

    /**
     * The ID of the channel to be sent the reminder.
     * <p>
     * If {@link #getChannelType()} returns {@link ChannelType#PRIVATE},
     * then this method will return the ID of the user who created the reminder.
     *
     * @return The ID of the channel to be sent the reminder.
     */
    public long getChannelId() {
        return get(REMINDERS.CHANNEL_ID);
    }

    public ChannelType getChannelType() {
        return ChannelType.valueOf(get(REMINDERS.CHANNEL_TYPE));
    }

    public String getMessage() {
        return get(REMINDERS.MESSAGE);
    }

    public ReminderType getType() {
        return ReminderType.valueOf(get(REMINDERS.TYPE));
    }

    /**
     * The value stored in the database.
     * The meaning of this value will depend on the {@linkplain #getType() reminder type}:
     * <ul>
     *   <li>{@link ReminderType#AT}
     *   <br>The time in the future that should be triggered this reminder.</li>
     *
     *   <li>{@link ReminderType#PERIOD}
     *   <br>The amount of seconds between two executions.</li>
     * </ul>
     * <b>Note:</b> Reminders ARE NOT limited to this simple value,
     * you should also check {@link #getExpression()}.
     *
     * @return The raw value stored in the reminder database column.
     * @see #getDuration()
     * @see #getExpression()
     */
    public Long getReminderValue() {
        return get(REMINDERS.REMINDER_VALUE);
    }

    public Duration getDuration() {
        Long remind = getReminderValue();

        if (getType().isExpression() || remind == null)
            throw new UnsupportedOperationException("The reminder type does not support durations " +
                    "(only expressions) or no duration was found, type: " + getType());

        return Duration.ofSeconds(remind);
    }

    public String getExpression() {
        return get(REMINDERS.EXPRESSION);
    }

    /**
     * Gets the amount of times this reminder should trigger.
     * <p>
     * If the execution is handled by another factor, like an {@linkplain #getExpression() expression},
     * then this value will always return {@code 1}.
     * <p>
     * {@code 0} will never be returned.
     * <p>
     * <b>Note:</b> If this reminder is handled by an {@linkplain #getExpression() expression}
     * and you want to check how many times it will be triggered, you can use
     * classes like {@link org.quartz.CronExpression CronExpression} to calculate it for you.
     *
     * @return The amount of times this reminder should be triggered.
     */
    public int getTriggerTimes() {
        Integer times = get(REMINDERS.TRIGGER_TIMES);
        if (getType().isExpression()) return -1;

        return times == null || times < 1 ? 1 : times;
    }

    public int getTriggersLeft() {
        if (getType().isExpression()) return -1;

        if (isExpired()) return 0;

        int left = get(REMINDERS.TRIGGERS_LEFT);
        return Math.max(left, 0);
    }

    public long getLastTimeTriggered() {
        return get(REMINDERS.LAST_TRIGGERED_AT);
    }

    public boolean isExpired() {
        return get(REMINDERS.EXPIRED);
    }

    public long getTimeCreated() {
        return get(REMINDERS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(REMINDERS.UPDATED_AT);
    }

    public Reminder setUserId(long userId) {
        set(REMINDERS.USER_ID, userId);
        return this;
    }

    public Reminder setChannelId(long channelId) {
        set(REMINDERS.CHANNEL_ID, channelId);
        return this;
    }

    public Reminder setChannelType(ChannelType channelType) {
        set(REMINDERS.CHANNEL_TYPE, channelType.name());
        return this;
    }

    public Reminder setMessage(String message) {
        set(REMINDERS.MESSAGE, message);
        return this;
    }

    public Reminder setType(ReminderType type) {
        set(REMINDERS.TYPE, type.name());
        return this;
    }

    public Reminder setReminderValue(long value) {
        set(REMINDERS.REMINDER_VALUE, value);
        return this;
    }

    public Reminder setExpression(String expression) {
        set(REMINDERS.EXPRESSION, expression);
        return this;
    }

    public Reminder setTriggerTimes(int times) {
        set(REMINDERS.TRIGGER_TIMES, times);
        return this;
    }

    public Reminder setTriggersLeft(int remain) {
        set(REMINDERS.TRIGGERS_LEFT, remain);
        return this;
    }

    public Reminder setLastTimeTriggered(long lastTimeTriggered) {
        set(REMINDERS.LAST_TRIGGERED_AT, lastTimeTriggered);
        return this;
    }

    public Reminder setExpired(boolean expired) {
        set(REMINDERS.EXPIRED, expired);
        return this;
    }

    @NotNull
    @Override
    public Reminder setLastUpdated(long timestamp) {
        set(REMINDERS.UPDATED_AT, timestamp);
        return this;
    }

    private void checkChannelType(ChannelType type) {
        if (!isChannelAllowed(type))
            throw new IllegalArgumentException("Channel type '" + type + "' is not supported");
    }
}