package ofc.bot.domain.entity.enums;

public enum AccumulatorPrizeStatus {
    PENDING,
    PAID,
    REJECTED;

    public static AccumulatorPrizeStatus fromName(String name) {
        if (name == null) {
            return null;
        }

        for (AccumulatorPrizeStatus status : values()) {
            if (status.name().equalsIgnoreCase(name)) {
                return status;
            }
        }
        return null;
    }
}
