package ofc.bot.handlers.games.mafia.domain;

/**
 * Holds the requested or resolved amount of each special role for a match.
 *
 * @param assassins total assassins in the match
 * @param doctors total doctors in the match
 * @param detectives total detectives in the match
 */
public record MafiaRoleConfiguration(int assassins, int doctors, int detectives) {
    /**
     * Returns the total amount of special-role slots.
     *
     * @return assassins + doctors + detectives
     */
    public int specialRoles() {
        return assassins + doctors + detectives;
    }

    /**
     * Calculates the amount of villagers left after all special roles are allocated.
     *
     * @param playerCount total amount of players in the match
     * @return remaining villager count
     */
    public int villagersFor(int playerCount) {
        return playerCount - specialRoles();
    }

    /**
     * Calculates the total amount of village-aligned players, including doctors and detectives.
     *
     * @param playerCount total amount of players in the match
     * @return village-aligned player count
     */
    public int villageTeamFor(int playerCount) {
        return playerCount - assassins;
    }
}
