package ofc.bot.handlers.shop;

import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.util.Bot;

public final class ColorRoleRefundPolicy {
    public static final long REFUND_PERIOD_SECONDS = 5 * 60;

    private ColorRoleRefundPolicy() {}

    public static boolean isRefundable(ColorRoleState state) {
        return isRefundable(state, Bot.unixNow());
    }

    public static boolean isRefundable(ColorRoleState state, long now) {
        return state != null && now - state.getTimeCreated() < REFUND_PERIOD_SECONDS;
    }
}
