package ofc.bot.handlers.games.mafia.enums;

public enum Role {
    VILLAGER("Aldeão"),
    ASSASSIN("Assassino"),
    DOCTOR("Médico"),
    DETECTIVE("Detetive"),;

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
