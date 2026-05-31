package ofc.bot.domain.entity.enums;

public enum AccumulatorPrizeType {
    COLOR_ROLE("Color Role", 1),
    MONEY("Money", 2);

    private final String display;
    private final int priority;

    AccumulatorPrizeType(String display, int priority) {
        this.display = display;
        this.priority = priority;
    }

    public String getDisplay() {
        return display;
    }

    public int getPriority() {
        return priority;
    }

    public static AccumulatorPrizeType fromName(String name) {
        if (name == null) {
            return null;
        }

        for (AccumulatorPrizeType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
