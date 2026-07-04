package ofc.bot.commands.impl.slash.groups;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroupInfoCommandTest {
    @Test
    void shouldFormatExactGroupMemberCount() {
        assertEquals("170.000", GroupInfoCommand.formatMemberCount(170_000));
    }

    @Test
    void shouldShowDisabledRent() {
        assertEquals("Desativado", GroupInfoCommand.formatRent());
    }
}
