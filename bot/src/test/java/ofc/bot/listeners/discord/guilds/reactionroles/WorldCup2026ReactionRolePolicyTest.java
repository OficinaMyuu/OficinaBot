package ofc.bot.listeners.discord.guilds.reactionroles;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldCup2026ReactionRolePolicyTest {
    @Test
    void shouldHandleOnlyConfiguredChannel() {
        assertTrue(WorldCup2026ReactionRolePolicy.isConfiguredChannel(123L, 123L));
        assertFalse(WorldCup2026ReactionRolePolicy.isConfiguredChannel(123L, 456L));
        assertFalse(WorldCup2026ReactionRolePolicy.isConfiguredChannel(null, 123L));
    }

    @Test
    void shouldHandleOnlySoccerBallEmoji() {
        assertTrue(WorldCup2026ReactionRolePolicy.isSoccerBall(Emoji.fromUnicode("\u26BD")));
        assertFalse(WorldCup2026ReactionRolePolicy.isSoccerBall(Emoji.fromUnicode("\uD83C\uDFC0")));
    }

    @Test
    void shouldAddRoleOnlyWhenMissingOnReactionAdd() {
        assertEquals(WorldCup2026ReactionRolePolicy.RoleUpdate.ADD,
                WorldCup2026ReactionRolePolicy.reactionAdded(false));
        assertEquals(WorldCup2026ReactionRolePolicy.RoleUpdate.NONE,
                WorldCup2026ReactionRolePolicy.reactionAdded(true));
    }

    @Test
    void shouldRemoveRoleOnlyWhenPresentOnReactionRemove() {
        assertEquals(WorldCup2026ReactionRolePolicy.RoleUpdate.REMOVE,
                WorldCup2026ReactionRolePolicy.reactionRemoved(true));
        assertEquals(WorldCup2026ReactionRolePolicy.RoleUpdate.NONE,
                WorldCup2026ReactionRolePolicy.reactionRemoved(false));
    }
}
