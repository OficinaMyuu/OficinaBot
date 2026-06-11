package ofc.bot.listeners.discord.guilds.reactionroles;

import net.dv8tion.jda.api.entities.emoji.Emoji;

final class WorldCup2026ReactionRolePolicy {
    static final String CHANNEL_CONFIG_KEY = "worldcup2026.channel_id";
    static final String ROLE_CONFIG_KEY = "worldcup2026.role_id";
    static final Emoji SOCCER_BALL = Emoji.fromUnicode("\u26BD");

    private WorldCup2026ReactionRolePolicy() {}

    static boolean isConfiguredChannel(Long configuredChannelId, long eventChannelId) {
        return configuredChannelId != null && configuredChannelId == eventChannelId;
    }

    static boolean isSoccerBall(Emoji emoji) {
        return SOCCER_BALL.equals(emoji);
    }

    static RoleUpdate reactionAdded(boolean memberHasRole) {
        return memberHasRole ? RoleUpdate.NONE : RoleUpdate.ADD;
    }

    static RoleUpdate reactionRemoved(boolean memberHasRole) {
        return memberHasRole ? RoleUpdate.REMOVE : RoleUpdate.NONE;
    }

    enum RoleUpdate {
        ADD,
        REMOVE,
        NONE
    }
}
