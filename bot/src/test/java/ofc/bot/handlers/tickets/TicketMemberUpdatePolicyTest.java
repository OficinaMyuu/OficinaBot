package ofc.bot.handlers.tickets;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketMemberUpdatePolicyTest {
    private static final long INITIATOR_ID = 10L;

    @Test
    void shouldApplyAddWhenUserDoesNotHaveTicketAccess() {
        TicketMemberUpdatePolicy.Candidate candidate = new TicketMemberUpdatePolicy.Candidate(
                20L,
                false,
                false,
                false
        );

        TicketMemberUpdatePolicy.Decision decision = TicketMemberUpdatePolicy.decide(
                TicketMemberAction.ADD,
                candidate,
                INITIATOR_ID
        );

        assertTrue(decision.shouldApply());
        assertNull(decision.reason());
    }

    @Test
    void shouldSkipAddWhenUserAlreadyHasTicketAccess() {
        TicketMemberUpdatePolicy.Candidate candidate = new TicketMemberUpdatePolicy.Candidate(
                20L,
                true,
                true,
                false
        );

        TicketMemberUpdatePolicy.Decision decision = TicketMemberUpdatePolicy.decide(
                TicketMemberAction.ADD,
                candidate,
                INITIATOR_ID
        );

        assertFalse(decision.shouldApply());
        assertEquals(TicketMemberUpdatePolicy.SkipReason.ALREADY_IN_TICKET, decision.reason());
    }

    @Test
    void shouldApplyRemoveWhenUserHasMemberOverride() {
        TicketMemberUpdatePolicy.Candidate candidate = new TicketMemberUpdatePolicy.Candidate(
                20L,
                true,
                true,
                false
        );

        TicketMemberUpdatePolicy.Decision decision = TicketMemberUpdatePolicy.decide(
                TicketMemberAction.REMOVE,
                candidate,
                INITIATOR_ID
        );

        assertTrue(decision.shouldApply());
        assertNull(decision.reason());
    }

    @Test
    void shouldSkipRemoveForTicketInitiator() {
        TicketMemberUpdatePolicy.Candidate candidate = new TicketMemberUpdatePolicy.Candidate(
                INITIATOR_ID,
                true,
                true,
                false
        );

        TicketMemberUpdatePolicy.Decision decision = TicketMemberUpdatePolicy.decide(
                TicketMemberAction.REMOVE,
                candidate,
                INITIATOR_ID
        );

        assertFalse(decision.shouldApply());
        assertEquals(TicketMemberUpdatePolicy.SkipReason.TICKET_INITIATOR, decision.reason());
    }

    @Test
    void shouldSkipRemoveForAdmins() {
        TicketMemberUpdatePolicy.Candidate candidate = new TicketMemberUpdatePolicy.Candidate(
                20L,
                true,
                true,
                true
        );

        TicketMemberUpdatePolicy.Decision decision = TicketMemberUpdatePolicy.decide(
                TicketMemberAction.REMOVE,
                candidate,
                INITIATOR_ID
        );

        assertFalse(decision.shouldApply());
        assertEquals(TicketMemberUpdatePolicy.SkipReason.ADMIN, decision.reason());
    }

    @Test
    void shouldSkipRemoveWhenUserHasNoMemberOverride() {
        TicketMemberUpdatePolicy.Candidate candidate = new TicketMemberUpdatePolicy.Candidate(
                20L,
                false,
                false,
                false
        );

        TicketMemberUpdatePolicy.Decision decision = TicketMemberUpdatePolicy.decide(
                TicketMemberAction.REMOVE,
                candidate,
                INITIATOR_ID
        );

        assertFalse(decision.shouldApply());
        assertEquals(TicketMemberUpdatePolicy.SkipReason.NOT_IN_TICKET, decision.reason());
    }

    @Test
    void shouldSummarizeAppliedAndSkippedSelections() {
        TicketMemberUpdatePolicy.Summary summary = TicketMemberUpdatePolicy.summarize(
                TicketMemberAction.ADD,
                List.of(
                        TicketMemberUpdatePolicy.Decision.apply(),
                        TicketMemberUpdatePolicy.Decision.skip(TicketMemberUpdatePolicy.SkipReason.ALREADY_IN_TICKET)
                )
        );

        assertEquals(1, summary.applied());
        assertEquals(1, summary.skipped());
        assertEquals("> 1 membro(s) adicionado(s) com sucesso. 1 seleção(ões) ignorada(s).", summary.toUserMessage());
    }
}
