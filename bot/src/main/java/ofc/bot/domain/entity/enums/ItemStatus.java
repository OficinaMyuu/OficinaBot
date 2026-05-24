package ofc.bot.domain.entity.enums;

public enum ItemStatus {
    PENDING,
    CONSUMED;

    public static ItemStatus fromName(String name) {
        for (ItemStatus itemStatus : ItemStatus.values()) {
            if (itemStatus.name().equalsIgnoreCase(name)) {
                return itemStatus;
            }
        }
        return null;
    }
}