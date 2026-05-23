package ofc.bot.handlers.shop;

import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.handlers.economy.CurrencyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorRoleRefundPolicyTest {
    @Test
    void shouldRefundBeforeFiveMinutes() {
        ColorRoleState state = state(1000L);

        assertTrue(ColorRoleRefundPolicy.isRefundable(state, 1299L));
    }

    @Test
    void shouldNotRefundAtFiveMinutesOrLater() {
        ColorRoleState state = state(1000L);

        assertFalse(ColorRoleRefundPolicy.isRefundable(state, 1300L));
        assertFalse(ColorRoleRefundPolicy.isRefundable(state, 1301L));
    }

    @Test
    void shouldNotRefundMissingState() {
        assertFalse(ColorRoleRefundPolicy.isRefundable(null, 1000L));
    }

    private ColorRoleState state(long createdAt) {
        return new ColorRoleState(
                500,
                CurrencyType.OFICINA,
                1L,
                2L,
                3L,
                2000L,
                createdAt,
                createdAt
        );
    }
}
