package ofc.bot.domain.entity.enums;

public enum NameScope {
    USERNAME("Username"),
    GLOBAL_NAME("Global Name"),
    GUILD_NICK("Server Nick");

    private final String displayName;

    NameScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * This method is preferred over {@code valueOf(String)}
     * since no exception is thrown if no constants are found.
     *
     * @param str The name of the constant.
     * @return the {@link NameScope} instance, {@code null} otherwise.
     */
    public static NameScope findByName(String str) {
        for (NameScope name : NameScope.values()) {
            if (name.toString().equalsIgnoreCase(str)) return name;
        }
        return null;
    }
}