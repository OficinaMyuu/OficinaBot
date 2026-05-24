package ofc.bot.handlers.economy;

import org.jetbrains.annotations.Nullable;

public class BankAction {
    public static final BankAction STATIC_SUCCESS_NO_CHANGE = new BankAction(true, false, null);
    public static final BankAction STATIC_FAILURE_NO_CHANGE = new BankAction(false, false, null);
    private final boolean successful;
    private final boolean changed;
    private final Runnable rollback;

    public BankAction(boolean successful, boolean changed, @Nullable Runnable rollback) {
        this.successful = successful;
        this.changed = changed;
        this.rollback = rollback;
    }

    public boolean isOk() {
        return this.successful;
    }

    /**
     * Checks if the action made any changes.
     *
     * @return {@code true} if something changed after this action,
     *         {@code false} otherwise.
     */
    public boolean changed() {
        return this.changed;
    }

    public void rollback() {
        if (rollback != null)
            rollback.run();
    }
}