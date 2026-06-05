package ofc.bot.listeners.discord.guilds.members;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.handlers.nick.NicknameEmojiEnforcer;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@DiscordEventHandler
public class MemberNameEmojiEnforcementListener extends ListenerAdapter {
    private static final String GUILD_NICK_REASON = "Removing unauthorized staff emoji from guild nickname.";
    private static final String GLOBAL_NAME_REASON = "Removing unauthorized staff emoji from global display name.";

    private final NicknameEmojiEnforcer enforcer;

    public MemberNameEmojiEnforcementListener(@NotNull NicknameEmojiEnforcer enforcer) {
        this.enforcer = Objects.requireNonNull(enforcer);
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        String fallbackName = displayName(member.getUser(), member.getUser().getGlobalName());
        enforcer.enforce(member, event.getNewNickname(), fallbackName, GUILD_NICK_REASON);
    }

    @Override
    public void onUserUpdateGlobalName(UserUpdateGlobalNameEvent event) {
        User user = event.getUser();
        String newDisplayName = displayName(user, event.getNewGlobalName());

        for (Guild guild : event.getJDA().getGuilds()) {
            Member member = guild.getMember(user);
            if (member == null || member.getNickname() != null) {
                continue;
            }

            enforcer.enforce(member, newDisplayName, user.getName(), GLOBAL_NAME_REASON);
        }
    }

    private static String displayName(User user, String globalName) {
        return globalName == null || globalName.isBlank() ? user.getName() : globalName;
    }
}
