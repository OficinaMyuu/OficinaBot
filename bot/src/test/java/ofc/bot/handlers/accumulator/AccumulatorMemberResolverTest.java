package ofc.bot.handlers.accumulator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccumulatorMemberResolverTest {
    @Test
    void shouldResolveMembersInGatewaySizedBatches() {
        List<List<Long>> batches = new ArrayList<>();
        AccumulatorMemberResolver resolver = new AccumulatorMemberResolver((guild, userIds) -> {
            batches.add(userIds);
            return Set.copyOf(userIds);
        });

        List<Long> ids = new ArrayList<>();
        for (long id = 1; id <= 205; id++) {
            ids.add(id);
        }

        Set<Long> foundIds = resolver.findExistingMemberIds(null, ids);

        assertEquals(new HashSet<>(ids), foundIds);
        assertEquals(3, batches.size());
        assertEquals(100, batches.get(0).size());
        assertEquals(100, batches.get(1).size());
        assertEquals(5, batches.get(2).size());
    }

    @Test
    void shouldDeduplicateBeforeResolvingMembers() {
        List<List<Long>> batches = new ArrayList<>();
        AccumulatorMemberResolver resolver = new AccumulatorMemberResolver((guild, userIds) -> {
            batches.add(userIds);
            return Set.copyOf(userIds);
        });

        Set<Long> foundIds = resolver.findExistingMemberIds(null, List.of(10L, 10L, 20L, 0L, -1L));

        assertEquals(Set.of(10L, 20L), foundIds);
        assertEquals(List.of(10L, 20L), batches.getFirst());
    }
}
