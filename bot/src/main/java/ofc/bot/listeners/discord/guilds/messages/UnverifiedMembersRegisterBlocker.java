package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Channels;

import java.util.List;

@DiscordEventHandler
public class UnverifiedMembersRegisterBlocker extends ListenerAdapter {
    private static final long VERIFIED_ROLE_ID = 758095502599520328L;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        final long chanId = Channels.REGISTRY.fetchIdLong();
        if (event.getChannel().getIdLong() != chanId) return;

        Member member = event.getMember();

        if (member == null) return;

        Message message = event.getMessage();
        List<Long> roles = member.getRoles()
                .stream()
                .map(Role::getIdLong)
                .toList();

        if (!roles.contains(VERIFIED_ROLE_ID) && !member.hasPermission(Permission.MESSAGE_MANAGE))
            Bot.delete(message);
    }
}