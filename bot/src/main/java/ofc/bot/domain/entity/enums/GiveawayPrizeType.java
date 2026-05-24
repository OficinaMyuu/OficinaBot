package ofc.bot.domain.entity.enums;

public enum GiveawayPrizeType {
    GENERIC("Genérico"),
    ECONOMY_MONEY("Dinheiro"),
    COLOR_ROLE("Cargo de cor");

    private final String displayName;

    GiveawayPrizeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GiveawayPrizeType fromOption(String value) {
        if (value == null) {
            return null;
        }

        for (GiveawayPrizeType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
