package ofc.bot.handlers.games.mafia.enums;

/**
 * Represents every role supported by Oficina Dorme.
 */
public enum MafiaRole {
    VILLAGER("Aldeão", MafiaTeam.VILLAGE, null),
    ASSASSIN("Assassino", MafiaTeam.ASSASSINS, "oficina-dorme-assassinos"),
    DOCTOR("Médico", MafiaTeam.VILLAGE, "oficina-dorme-medicos"),
    DETECTIVE("Detetive", MafiaTeam.VILLAGE, "oficina-dorme-detetives");

    private final String displayName;
    private final MafiaTeam team;
    private final String privateThreadName;

    MafiaRole(String displayName, MafiaTeam team, String privateThreadName) {
        this.displayName = displayName;
        this.team = team;
        this.privateThreadName = privateThreadName;
    }

    /**
     * Returns the localized role name shown to Discord users.
     *
     * @return pt-BR display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the team that owns the role.
     *
     * @return role team
     */
    public MafiaTeam getTeam() {
        return team;
    }

    /**
     * Returns the private thread name used for secret coordination roles.
     *
     * @return private thread name, or {@code null} when the role has no thread
     */
    public String getPrivateThreadName() {
        return privateThreadName;
    }

    /**
     * Indicates whether the role should receive a private thread.
     *
     * @return {@code true} for assassins, doctors, and detectives
     */
    public boolean hasPrivateThread() {
        return privateThreadName != null;
    }
}
