package ofc.bot.handlers.accumulator;

import ofc.bot.handlers.accumulator.AccumulatorImportPlanner.DuplicatePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccumulatorImportPlannerTest {
    private final AccumulatorImportPlanner planner = new AccumulatorImportPlanner();

    @Test
    void shouldExposeValidTargetIdsForBatchMemberLookup() {
        List<Long> ids = planner.validTargetIds("""
                123
                nope
                -1
                456
                123
                """);

        assertEquals(List.of(123L, 456L, 123L), ids);
    }

    @Test
    void shouldAllowDuplicatesWhenPolicyAllowsDuplicates() {
        var plan = planner.plan("""
                123
                123
                456
                """, DuplicatePolicy.ALLOW, Set.of(123L), id -> true);

        assertEquals(3, plan.totalIds());
        assertEquals(java.util.List.of(123L, 123L, 456L), plan.acceptedIds());
        assertTrue(plan.errors().isEmpty());
    }

    @Test
    void shouldForbidPayloadDuplicatesAndExistingPendingPrizes() {
        var plan = planner.plan("""
                123
                123
                456
                """, DuplicatePolicy.FORBID, Set.of(456L), id -> true);

        assertEquals(java.util.List.of(123L), plan.acceptedIds());
        assertEquals(2, plan.errors().size());
        assertTrue(plan.errors().get(0).contains("duplicado"));
        assertTrue(plan.errors().get(1).contains("pendente"));
    }

    @Test
    void shouldRejectInvalidLinesAndMissingMembers() {
        var plan = planner.plan("""
                nope
                -1
                123
                456
                """, DuplicatePolicy.ALLOW, Set.of(), id -> id == 456L);

        assertEquals(java.util.List.of(456L), plan.acceptedIds());
        assertEquals(3, plan.errors().size());
        assertTrue(plan.errors().get(0).contains("ID inválido"));
        assertTrue(plan.errors().get(1).contains("ID inválido"));
        assertTrue(plan.errors().get(2).contains("membro não encontrado"));
    }
}
