package ofc.bot.listeners.discord.guilds.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToggleRankupPingsCommandHandlerTest {
    @Test
    void shouldDescribeEnabledState() {
        assertEquals("Pings de rank up ativados.", ToggleRankupPingsCommandHandler.responseFor(true));
    }

    @Test
    void shouldDescribeDisabledState() {
        assertEquals("Pings de rank up desativados.", ToggleRankupPingsCommandHandler.responseFor(false));
    }
}
