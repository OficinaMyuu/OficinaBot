package ofc.bot.handlers.interactions;

public enum AutoResponseType {
    NONE(false),
    DEFER_EDIT(false),
    THINKING(false),
    THINKING_EPHEMERAL(true);

    private final boolean ephemeral;

    AutoResponseType(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public boolean isEphemeral() {
        return this.ephemeral;
    }
}