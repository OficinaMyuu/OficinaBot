package ofc.bot.handlers.registration;

public enum RegistrationGender {
    MALE("Male", RegistrationRole.MALE),
    FEMALE("Female", RegistrationRole.FEMALE),
    NON_BINARY("Non-Binary", RegistrationRole.NON_BINARY);

    private final String label;
    private final RegistrationRole role;

    RegistrationGender(String label, RegistrationRole role) {
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
