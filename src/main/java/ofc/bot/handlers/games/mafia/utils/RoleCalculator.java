package ofc.bot.handlers.games.mafia.utils;

import ofc.bot.handlers.games.mafia.models.MatchState;

public class RoleCalculator {
    public static RoleDistribution calculate(int totalPlayers) {
        if (totalPlayers < MatchState.MIN_PLAYERS) {
            throw new IllegalArgumentException("The game requires at least " + MatchState.MIN_PLAYERS +
                    " players to start, but only " + totalPlayers + " were found.");
        }

        int assassins = (totalPlayers / 5) - 1;
        int doctors = (totalPlayers / 5) - 1;
        int detectives = totalPlayers / 10;
        int villagers = totalPlayers - assassins - doctors - detectives;

        return new RoleDistribution(assassins, doctors, detectives, villagers);
    }

    public record RoleDistribution(int assassins, int doctors, int detectives, int villagers) {}
}