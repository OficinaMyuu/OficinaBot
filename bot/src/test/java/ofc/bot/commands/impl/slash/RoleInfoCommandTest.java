package ofc.bot.commands.impl.slash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RoleInfoCommandTest {
    @Test
    void shouldFormatOnlyTotalMemberCount() {
        String field = RoleInfoCommand.formatMemberField(170_000);

        assertEquals("Total: `170.000`", field);
        assertFalse(field.contains("Online"));
    }
}
