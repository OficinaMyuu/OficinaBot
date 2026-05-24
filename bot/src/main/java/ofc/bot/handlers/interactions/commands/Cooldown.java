package ofc.bot.handlers.interactions.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public record Cooldown(boolean managerBypass, boolean global, long waitSeconds) {
    public static final Cooldown ZERO = new Cooldown(true, false, 0);

    public Cooldown {
        Checks.notNegative(waitSeconds, "Wait Seconds");
    }

    public static Cooldown of(boolean managerBypass, boolean global, long period, TimeUnit unit) {
        return new Cooldown(managerBypass, global, unit.toSeconds(period));
    }

    public static Cooldown of(boolean managerBypass, long period, TimeUnit unit) {
        return new Cooldown(managerBypass, false, unit.toSeconds(period));
    }

    public static Cooldown of(long period, TimeUnit unit) {
        return of(true, period, unit);
    }

    /**
     * Determines whether the specified member is subject to this cooldown instance.
     * <p>
     * <b>Note:</b> This method does not perform database checks. It only verifies if the member
     * can be affected by this cooldown, not whether they have actually used it before.
     * As a result, this method may return {@code true} even if the member has never used this resource.
     *
     * @param member The {@link Member} to check.
     * @return {@code true} if the member may be affected by this cooldown instance, {@code false} otherwise.
     */
    public boolean isAffected(@NotNull Member member) {
        Checks.notNull(member, "Member");

        return !(this.managerBypass && member.hasPermission(Permission.MANAGE_SERVER));
    }

    /**
     * Checks whether this {@code member} is currently rate-limited on this resource.
     * <p>
     * <b>Note:</b> This method DOES check permissions and bypass rules.
     *
     * @param member The {@link Member} to be checked.
     * @param lastUsed The last time this member has called this resource.
     * @return {@code true} if the user is currently rate-limited, {@code false} otherwise.
     */
    public boolean isRateLimited(@NotNull Member member, long lastUsed) {
        return getWaitSeconds(member, lastUsed) > 0;
    }

    /**
     * Gets the amount of time this {@code member} must wait before using this resource again.
     *
     * @param member The {@link Member} to be checked.
     * @return The amount of seconds to wait, before proceeding to call this same resource,
     *         or {@code 0} if there is no time to wait.
     */
    public long getWaitSeconds(@NotNull Member member, long lastUsed) {
        Checks.notNull(member, "Member");

        if (this.waitSeconds <= 0 || !isAffected(member)) return 0;

        long now = Bot.unixNow();
        long nextCall = lastUsed + this.waitSeconds;
        return Math.max(nextCall - now, 0);
    }

    public long getNextCall(@NotNull Member member, long lastUsed) {
        Checks.notNull(member, "Member");

        return this.waitSeconds <= 0 || !isAffected(member) ? 0 : Math.max(lastUsed + this.waitSeconds, 0);
    }

    public boolean isZero() {
        return waitSeconds <= 0;
    }
}