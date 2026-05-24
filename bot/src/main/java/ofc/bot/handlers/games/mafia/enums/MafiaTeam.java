package ofc.bot.handlers.games.mafia.enums;

/**
 * Represents the two teams that can win a match.
 */
public enum MafiaTeam {
    VILLAGE("Aldeia"),
    ASSASSINS("Assassinos");

    private final String displayName;

    MafiaTeam(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the pt-BR name shown to Discord users.
     *
     * @return localized display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
