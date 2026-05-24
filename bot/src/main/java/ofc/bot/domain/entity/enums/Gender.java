package ofc.bot.domain.entity.enums;

import org.jetbrains.annotations.Nullable;

public enum Gender {
    MALE('o'),
    FEMALE('a'),
    UNKNOWN('o'); // In Portuguese (Brazil), the neutral form is the male form

    private final char suffix;

    Gender(char suffix) {
        this.suffix = suffix;
    }

    /**
     * Returns the gendered suffix (e.g., 'o' or 'a') commonly used in adjectives or
     * nouns in Brazilian Portuguese.
     * <p>
     * This suffix is typically used to generate sentences like "bonito", "bonita", etc.
     * However, it may not always apply directly, as some words in Portuguese may default
     * to the feminine form regardless of gender, or require sentence restructuring for
     * gender neutrality.
     * Therefore, this should be seen as a guideline, not an absolute rule.
     *
     * @return A character representing the grammatical suffix for this gender.
     */
    public char getSuffix() {
        return this.suffix;
    }

    /**
     * Returns the the {@link Gender} enum constant based on the name,
     * or {@code null} if none is found.
     * <p>
     * This method follows the same logic as {@link #valueOf(String)}, but
     * no exception is thrown if the {@code name} does not match any constants.
     *
     * @param name The constant name to be returned (case-sensitive).
     * @return A {@link Gender} constant that matches the exact same name provided, {@code null} otherwise.
     */
    @Nullable
    public static Gender fromName(final String name) {
        for (final Gender gender : Gender.values()) {
            if (gender.name().equals(name)) {
                return gender;
            }
        }
        return null;
    }
}