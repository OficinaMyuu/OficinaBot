package ofc.bot.handlers.games.mafia.service;

import ofc.bot.handlers.games.mafia.domain.*;
import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import ofc.bot.handlers.games.mafia.enums.MafiaTeam;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MafiaMatchEngineTest {
    private final MafiaMatchEngine engine = new MafiaMatchEngine();

    @Test
    void shouldAutoBalanceSmallMatch() {
        MafiaRoleConfiguration configuration = engine.autoBalance(6);

        assertEquals(2, configuration.assassins());
        assertEquals(1, configuration.doctors());
        assertEquals(1, configuration.detectives());
        assertEquals(2, configuration.villagersFor(6));
    }

    @Test
    void shouldRejectConfigurationWithoutVillagers() {
        MafiaRoleConfiguration configuration = new MafiaRoleConfiguration(2, 2, 2);

        Optional<String> validation = engine.validateConfiguration(configuration, 6);

        assertTrue(validation.isPresent());
        assertTrue(validation.get().contains("aldeão"));
    }

    @Test
    void shouldBlockInvestigationAndAllowDoctorSelfDeath() {
        MafiaMatch match = new MafiaMatch(1L, 2L, 3L, 6, null);
        match.addPlayer(10L);
        match.addPlayer(11L);
        match.addPlayer(12L);
        match.addPlayer(13L);
        match.addPlayer(14L);
        match.addPlayer(15L);

        assign(match, 10L, MafiaRole.ASSASSIN);
        assign(match, 11L, MafiaRole.ASSASSIN);
        assign(match, 12L, MafiaRole.DOCTOR);
        assign(match, 13L, MafiaRole.DETECTIVE);
        assign(match, 14L, MafiaRole.VILLAGER);
        assign(match, 15L, MafiaRole.VILLAGER);

        match.getAssassinVotes().put(10L, 12L);
        match.getAssassinVotes().put(11L, 14L);
        match.getDoctorVotes().put(12L, 12L);
        match.getDetectiveVotes().put(13L, 12L);

        NightResolution resolution = engine.resolveNight(match);

        assertTrue(resolution.killedPlayerIds().contains(12L));
        assertTrue(resolution.killedPlayerIds().contains(14L));
        assertTrue(resolution.investigationsByDetective().get(13L).blocked());
        assertFalse(match.getPlayer(12L).isAlive());
    }

    @Test
    void shouldProtectTargetFromDeathAndInvestigation() {
        MafiaMatch match = new MafiaMatch(1L, 2L, 3L, 6, null);
        match.addPlayer(10L);
        match.addPlayer(11L);
        match.addPlayer(12L);
        match.addPlayer(13L);
        match.addPlayer(14L);
        match.addPlayer(15L);

        assign(match, 10L, MafiaRole.ASSASSIN);
        assign(match, 11L, MafiaRole.ASSASSIN);
        assign(match, 12L, MafiaRole.DOCTOR);
        assign(match, 13L, MafiaRole.DETECTIVE);
        assign(match, 14L, MafiaRole.VILLAGER);
        assign(match, 15L, MafiaRole.VILLAGER);

        match.getAssassinVotes().put(10L, 14L);
        match.getAssassinVotes().put(11L, 15L);
        match.getDoctorVotes().put(12L, 14L);
        match.getDetectiveVotes().put(13L, 14L);

        NightResolution resolution = engine.resolveNight(match);

        assertFalse(resolution.killedPlayerIds().contains(14L));
        assertTrue(resolution.killedPlayerIds().contains(15L));
        assertTrue(resolution.investigationsByDetective().get(13L).blocked());
        assertTrue(match.getPlayer(14L).isAlive());
    }

    @Test
    void shouldResolveDayTieWithoutElimination() {
        DayResolution resolution = engine.resolveDay(java.util.Map.of(
                10L, 20L,
                11L, 21L,
                12L, 20L,
                13L, 21L
        ));

        assertTrue(resolution.tie());
        assertNull(resolution.eliminatedPlayerId());
    }

    @Test
    void shouldDetectVillageAndAssassinVictories() {
        MafiaMatch villageWinMatch = new MafiaMatch(1L, 2L, 3L, 6, null);
        villageWinMatch.addPlayer(10L);
        villageWinMatch.addPlayer(11L);
        assign(villageWinMatch, 10L, MafiaRole.VILLAGER);
        assign(villageWinMatch, 11L, MafiaRole.DOCTOR);

        MafiaMatch assassinWinMatch = new MafiaMatch(1L, 2L, 3L, 6, null);
        assassinWinMatch.addPlayer(20L);
        assassinWinMatch.addPlayer(21L);
        assassinWinMatch.addPlayer(22L);
        assassinWinMatch.addPlayer(23L);
        assign(assassinWinMatch, 20L, MafiaRole.ASSASSIN);
        assign(assassinWinMatch, 21L, MafiaRole.ASSASSIN);
        assign(assassinWinMatch, 22L, MafiaRole.VILLAGER);
        assign(assassinWinMatch, 23L, MafiaRole.DOCTOR);

        assertEquals(Optional.of(MafiaTeam.VILLAGE), engine.determineWinner(villageWinMatch.getPlayers()));
        assertEquals(Optional.of(MafiaTeam.ASSASSINS), engine.determineWinner(assassinWinMatch.getPlayers()));
    }

    private void assign(MafiaMatch match, long userId, MafiaRole role) {
        match.getPlayer(userId).setRole(role);
    }
}
