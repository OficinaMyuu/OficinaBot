package ofc.bot.domain.entity.enums;

public enum PunishmentType {
    /**
     * This constant is sort of an alias to {@link #MUTE},
     * thus why it supports a duration (through {@link #supportsDuration() PunishmentType.supportsDuration()}).
     */
    WARN(  "advertido",     true),
    MUTE(  "silenciado",    true),
    UNMUTE("dessilenciado", true),
    KICK(  "expulso",       false),
    BAN(   "banido",        true),
    UNBAN( "desbanido",     false);

    private final String display;
    private final boolean supportsDuration;

    PunishmentType(String display, boolean supportsDuration) {
        this.display = display;
        this.supportsDuration = supportsDuration;
    }

    public String getDisplay() {
        return this.display;
    }

    /**
     * Checks whether this {@link PunishmentType} supports the {@code duration}
     * field or not.
     *
     * @return {@code true} if the punishment type supports a duration, {@code false} otherwise.
     */
    public boolean supportsDuration() {
        return this.supportsDuration;
    }
}