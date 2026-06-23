package ofc.bot.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OficinaGroupTest {
    private static final String BRAILLE_BLANK = "\u2800";

    @Test
    void shouldFormatRoleNameWithoutEmoji() {
        String roleName = OficinaGroup.formatRoleName("Piratas", "🏴", false);

        assertEquals(BRAILLE_BLANK.repeat(6) + "Piratas" + BRAILLE_BLANK.repeat(6), roleName);
    }

    @Test
    void shouldFormatRoleNameWithEmoji() {
        String roleName = OficinaGroup.formatRoleName("Piratas", "🏴", true);

        assertEquals("🏴" + BRAILLE_BLANK.repeat(4) + "Piratas" + BRAILLE_BLANK.repeat(4) + "🏴", roleName);
    }
}
