package ofc.bot.listeners.discord.guilds.voice.solo;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.Channels;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordEventHandler
public class SoloChannelsHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoloChannelsHandler.class);
    private static final long PARENT_ID = 691194660902928435L;
    private static int digit = 0;

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {
        Member member = e.getMember();
        AudioChannelUnion join = e.getChannelJoined();
        AudioChannelUnion left = e.getChannelLeft();
        long soloId = Channels.SOLO_GATEWAY.fetchIdLong();

        if (join != null && join.getIdLong() == soloId && join.getParentCategoryIdLong() == PARENT_ID) {
            handleJoin(member, join.asVoiceChannel());
        }

        // We cannot delete the SOLO_GATEWAY channel.
        if (left != null && left.getIdLong() != soloId && left.getParentCategoryIdLong() == PARENT_ID) {
            handleLeave(left.asVoiceChannel());
        }
    }

    private void handleJoin(Member member, VoiceChannel chan) {
        Guild guild = member.getGuild();
        Role publicRole = guild.getPublicRole();
        Category parent = chan.getParentCategory();

        // This null check will always fail, but I put this here to remove IDE warnings
        if (hitMaxChannels(guild) || parent == null) {
            guild.kickVoiceMember(member).queue();
            return;
        }

        String newSoloname = String.format("\uD83D\uDE49・Solo %02d", ++digit);
        parent.createVoiceChannel(newSoloname)
                .setUserlimit(1)
                .addRolePermissionOverride(publicRole.getIdLong(), List.of(), List.of(Permission.VOICE_SPEAK))
                .flatMap((newChan) -> guild.moveVoiceMember(member, newChan))
                .queue(null, (err) -> {
                    LOGGER.error("Could not create solo channel", err);
                    guild.kickVoiceMember(member).queue();
                });
    }

    private void handleLeave(VoiceChannel chan) {
        List<Member> connected = chan.getMembers();

        if (connected.isEmpty()) {
            chan.delete().queue();
        }
    }

    private boolean hitMaxChannels(Guild guild) {
        // Magic value (500): the max amount of channels a Guild can hold
        return guild.getChannels().size() >= 500;
    }
}