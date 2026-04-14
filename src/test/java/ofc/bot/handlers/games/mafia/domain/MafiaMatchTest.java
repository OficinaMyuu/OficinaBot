package ofc.bot.handlers.games.mafia.domain;

import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MafiaMatchTest {
    @Test
    void shouldRemoveVotesCastByAndTargetingRemovedPlayer() {
        MafiaMatch match = createMatchWithSixPlayers();

        match.getAssassinVotes().put(10L, 12L);
        match.getDoctorVotes().put(12L, 10L);
        match.getDetectiveVotes().put(13L, 12L);
        match.getDayVotes().put(14L, 12L);

        boolean removed = match.removePlayer(12L);

        assertTrue(removed);
        assertFalse(match.hasPlayer(12L));
        assertTrue(match.getAssassinVotes().isEmpty());
        assertTrue(match.getDoctorVotes().isEmpty());
        assertTrue(match.getDetectiveVotes().isEmpty());
        assertTrue(match.getDayVotes().isEmpty());
    }

    @Test
    void shouldRecalculateNightCompletionAfterRemovingPendingActor() {
        MafiaMatch match = createMatchWithSixPlayers();
        match.startNightPhase();

        match.getAssassinVotes().put(10L, 14L);
        match.getAssassinVotes().put(11L, 15L);
        match.getDetectiveVotes().put(13L, 14L);

        assertFalse(match.hasAllNightActionsSubmitted());

        boolean removed = match.removePlayer(12L);

        assertTrue(removed);
        assertTrue(match.hasAllNightActionsSubmitted());
    }

    @Test
    void shouldReportManagedChannels() {
        MafiaMatch match = createMatchWithSixPlayers();
        match.setPrivateThreadId(MafiaRole.ASSASSIN, 100L);
        match.setPrivateThreadId(MafiaRole.DOCTOR, 101L);

        assertTrue(match.managesChannel(2L));
        assertTrue(match.managesChannel(100L));
        assertTrue(match.managesChannel(101L));
        assertFalse(match.managesChannel(999L));
    }

    private MafiaMatch createMatchWithSixPlayers() {
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
        return match;
    }

    private void assign(MafiaMatch match, long userId, MafiaRole role) {
        match.getPlayer(userId).setRole(role);
    }
}
