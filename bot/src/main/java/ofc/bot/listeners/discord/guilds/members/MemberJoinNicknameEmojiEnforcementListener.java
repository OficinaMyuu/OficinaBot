package ofc.bot.listeners.discord.guilds.members;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.handlers.nick.NicknameEmojiEnforcer;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@DiscordEventHandler
public class MemberJoinNicknameEmojiEnforcementListener extends ListenerAdapter {
    private static final String JOIN_REASON = "Removing unauthorized staff emoji from joining member display name.";

    private final NicknameEmojiEnforcer enforcer;

    public MemberJoinNicknameEmojiEnforcementListener(@NotNull NicknameEmojiEnforcer enforcer) {
        this.enforcer = Objects.requireNonNull(enforcer);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        User user = member.getUser();
        String candidateName = member.getNickname();
        String fallbackName = user.getGlobalName() == null || user.getGlobalName().isBlank()
                ? user.getName()
                : user.getGlobalName();

        enforcer.enforce(member, candidateName, fallbackName, JOIN_REASON);
    }
}
