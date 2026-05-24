package ofc.bot.handlers.economy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomatedMoneyGainPolicyTest {
    @Test
    void blocksUserWithMoneyGainPolicy() {
        AutomatedMoneyGainPolicy policy = new AutomatedMoneyGainPolicy(() -> Set.of(10L));

        assertTrue(policy.isBlocked(10L, List.of(20L), 30L));
    }

    @Test
    void blocksRoleWithMoneyGainPolicy() {
        AutomatedMoneyGainPolicy policy = new AutomatedMoneyGainPolicy(() -> Set.of(20L));

        assertTrue(policy.isBlocked(10L, List.of(20L, 21L), 30L));
    }

    @Test
    void blocksChannelWithMoneyGainPolicy() {
        AutomatedMoneyGainPolicy policy = new AutomatedMoneyGainPolicy(() -> Set.of(30L));

        assertTrue(policy.isBlocked(10L, List.of(20L), 30L));
    }

    @Test
    void allowsWhenNoPolicyMatches() {
        AutomatedMoneyGainPolicy policy = new AutomatedMoneyGainPolicy(() -> Set.of(99L));

        assertFalse(policy.isBlocked(10L, List.of(20L), 30L));
    }
}
