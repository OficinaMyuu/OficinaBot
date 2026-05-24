package ofc.bot.domain.entity.enums;

public enum ReminderType {
    /**
     * Triggers at a specific date in the future.
     * One-time reminder.
     */
    AT("Data Fixa", false, false),

    /**
     * Triggers after a given period (usually in seconds or minutes).
     */
    PERIOD("Periódico", false, true),

    /**
     * Triggers based on a cron expression, offering advanced scheduling.
     */
    CRON("Cronológico", true, false);

    private final String name;
    private final boolean isExpression;
    private final boolean isRepeatable;

    ReminderType(String name, boolean isExpression, boolean isRepeatable) {
        this.name = name;
        this.isExpression = isExpression;
        this.isRepeatable = isRepeatable;
    }

    public String getName() {
        return this.name;
    }

    public boolean isExpression() {
        return this.isExpression;
    }

    public boolean isRepeatable() {
        return this.isRepeatable;
    }
}