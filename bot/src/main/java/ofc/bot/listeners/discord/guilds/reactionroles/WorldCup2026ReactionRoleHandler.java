package ofc.bot.listeners.discord.guilds.reactionroles;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@DiscordEventHandler
public class WorldCup2026ReactionRoleHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCup2026ReactionRoleHandler.class);

    private final Set<String> reportedConfigProblems = ConcurrentHashMap.newKeySet();

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (!e.isFromGuild()) return;

        Long channelId = configuredSnowflake(WorldCup2026ReactionRolePolicy.CHANNEL_CONFIG_KEY);
        if (!WorldCup2026ReactionRolePolicy.isConfiguredChannel(channelId, e.getChannel().getIdLong())) return;

        e.getMessage().addReaction(WorldCup2026ReactionRolePolicy.SOCCER_BALL).queue(
                null,
                err -> LOGGER.warn("Failed to add World Cup 2026 reaction to message {}", e.getMessageId(), err)
        );
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent e) {
        handleReactionRoleUpdate(e, true);
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent e) {
        handleReactionRoleUpdate(e, false);
    }

    private void handleReactionRoleUpdate(GenericMessageReactionEvent e, boolean added) {
        if (!shouldHandleReaction(e)) return;
        if (e.getUserIdLong() == e.getJDA().getSelfUser().getIdLong()) return;

        Long roleId = configuredSnowflake(WorldCup2026ReactionRolePolicy.ROLE_CONFIG_KEY);
        if (roleId == null) return;

        Guild guild = e.getGuild();
        Role role = guild.getRoleById(roleId);

        if (role == null) {
            logConfigProblem(
                    WorldCup2026ReactionRolePolicy.ROLE_CONFIG_KEY + ":not-found:" + roleId,
                    "Configured World Cup 2026 role {} was not found in guild {}",
                    roleId,
                    guild.getId()
            );
            return;
        }

        e.retrieveMember().queue(
                member -> updateMemberRole(guild, member, role, added),
                err -> LOGGER.warn("Failed to fetch World Cup 2026 reaction member {}", e.getUserId(), err)
        );
    }

    private boolean shouldHandleReaction(GenericMessageReactionEvent e) {
        if (!e.isFromGuild()) return false;
        if (!WorldCup2026ReactionRolePolicy.isSoccerBall(e.getEmoji())) return false;

        Long channelId = configuredSnowflake(WorldCup2026ReactionRolePolicy.CHANNEL_CONFIG_KEY);
        return WorldCup2026ReactionRolePolicy.isConfiguredChannel(channelId, e.getChannel().getIdLong());
    }

    private void updateMemberRole(Guild guild, Member member, Role role, boolean added) {
        if (member.getUser().isBot()) return;

        boolean memberHasRole = member.getRoles().contains(role);
        WorldCup2026ReactionRolePolicy.RoleUpdate update = added
                ? WorldCup2026ReactionRolePolicy.reactionAdded(memberHasRole)
                : WorldCup2026ReactionRolePolicy.reactionRemoved(memberHasRole);

        switch (update) {
            case ADD -> guild.addRoleToMember(member, role).queue(
                    null,
                    err -> LOGGER.warn("Failed to add World Cup 2026 role {} to member {}", role.getId(), member.getId(), err)
            );
            case REMOVE -> guild.removeRoleFromMember(member, role).queue(
                    null,
                    err -> LOGGER.warn("Failed to remove World Cup 2026 role {} from member {}", role.getId(), member.getId(), err)
            );
            case NONE -> {
            }
        }
    }

    private Long configuredSnowflake(String key) {
        String raw = Bot.get(key);

        if (raw == null || raw.isBlank()) {
            logConfigProblem(key + ":missing", "Missing World Cup 2026 config value for {}", key);
            return null;
        }

        try {
            return Long.parseLong(raw.strip());
        } catch (NumberFormatException e) {
            logConfigProblem(key + ':' + raw, "Invalid World Cup 2026 config value for {}: {}", key, raw);
            return null;
        }
    }

    private void logConfigProblem(String problemKey, String message, Object... args) {
        if (reportedConfigProblems.add(problemKey)) {
            LOGGER.warn(message, args);
        }
    }
}
