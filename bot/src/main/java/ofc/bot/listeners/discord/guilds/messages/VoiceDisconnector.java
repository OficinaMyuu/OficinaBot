package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class VoiceDisconnector extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();
        Member member = e.getMember();
        String content = msg.getContentRaw();

        if (!content.startsWith(".dd") || member == null) return;

        GuildVoiceState voiceState = member.getVoiceState();
        Guild guild = member.getGuild();

        // If the user is not connected to a voice channel,
        // we ignore this "command" call, but delete the message anyways
        if (voiceState != null && voiceState.inAudioChannel()) {
            guild.kickVoiceMember(member).queue();
        }
        Bot.delete(msg);
    }
}