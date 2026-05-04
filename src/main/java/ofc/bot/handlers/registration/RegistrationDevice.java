package ofc.bot.handlers.registration;

public enum RegistrationDevice {
    MOBILE("Mobile", RegistrationRole.MOBILE),
    DESKTOP("PC", RegistrationRole.DESKTOP);

    private final String label;
    private final RegistrationRole role;

    RegistrationDevice(String label, RegistrationRole role) {
        this.label = label;
        this.role = role;
    }

    public String label() {
        return this.label;
    }

    public RegistrationRole role() {
        return this.role;
    }
}
