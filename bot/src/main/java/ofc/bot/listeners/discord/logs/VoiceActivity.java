package ofc.bot.listeners.discord.logs;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.content.Channels;
import ofc.bot.util.content.Staff;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;

@DiscordEventHandler
public class VoiceActivity extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceActivity.class);
    private static final List<String> REQUIRED_ROLES = Staff.getIdsByScope(Staff.Scope.MOV_CALL);
    private static final List<Long> EXCLUDED_CHANNELS = List.of(1065077982588305538L, 693627612454453250L);
    
    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        AudioChannelUnion channelJoined = event.getChannelJoined();
        AudioChannelUnion channelLeft = event.getChannelLeft();
        Member member = event.getMember();

        if (isChannelExcluded(channelJoined) || isChannelExcluded(channelLeft)) return;

        if (!shouldLog(member)) return;

        // Leave
        if (channelJoined == null)
            send(null, channelLeft, event, VoiceAction.LEAVE);

        // Join
        else if (channelLeft == null)
            send(channelJoined, null, event, VoiceAction.JOIN);

        // Move
        else
            send(channelJoined, channelLeft, event, VoiceAction.MOVE);
    }

    private boolean isChannelExcluded(AudioChannel chan) {
        return chan != null && EXCLUDED_CHANNELS.contains(chan.getIdLong());
    }

    @SuppressWarnings("DataFlowIssue")
    private void send(AudioChannel join, AudioChannel leave, GuildVoiceUpdateEvent event, VoiceAction action) {
        EmbedBuilder builder = new EmbedBuilder();
        Member member = event.getMember();
        User user = member.getUser();
        Guild guild = event.getGuild();
        TextChannel log = Channels.MOV_CALL_LOG.textChannel();
        int joinAmount = join == null ? 0 : join.getMembers().size();
        int leaveAmount = leave == null ? 0 : leave.getMembers().size();

        if (log == null) {
            LOGGER.warn("Could not find log channel for id {}", Channels.MOV_CALL_LOG.fetchId());
            return;
        }
        builder.setFooter(guild.getName(), guild.getIconUrl());

        switch (action) {
            case JOIN -> builder
                    .setAuthor(user.getName() + " entrou em " + join.getName(), null, user.getEffectiveAvatarUrl())
                    .addField("👑 Staff",      member.getAsMention(),             true)
                    .addField("👥 Conectados", String.format("%02d", joinAmount), true)
                    .addField("🔊 Canal",      join.getAsMention(),               true)
                    .setColor(Color.GREEN);

            case LEAVE -> builder
                    .setAuthor(user.getName() + " saiu de " + leave.getName(), null, user.getEffectiveAvatarUrl())
                    .addField("👑 Staff",      member.getAsMention(),              true)
                    .addField("👥 Conectados", String.format("%02d", leaveAmount), true)
                    .addField("🔊 Canal",      leave.getAsMention(),               true)
                    .setColor(Color.RED);

            case MOVE -> builder
                    .setAuthor(user.getName() + " se moveu para " + join.getName(), null, user.getEffectiveAvatarUrl())
                    .addField("🔈 Saiu de",     leave.getAsMention(),                                                  true)
                    .addField("👑 Staff",      member.getAsMention(),                                                 true)
                    .addField("👥 Conectados", String.format("Anterior: %02d\nAtual: %02d", leaveAmount, joinAmount), true)
                    .addField("🔊 Canais",     String.format("%s -> %s", leave.getAsMention(), join.getAsMention()),  true)
                    .setColor(Color.YELLOW);
        }
        send(log, join, leave, member, builder.build());
    }
    
    private void send(TextChannel log, AudioChannel join, AudioChannel leave, Member target, MessageEmbed embed) {
        log.sendMessageEmbeds(embed).queue(null, (err) -> {
            LOGGER.error("Could not log activity of member @{} [{}] from \"{}\" to \"{}\"",
                    target.getUser().getName(),
                    target.getId(),
                    leave == null ? "unknown" : leave.getId(),
                    join == null  ? "unknown" : join.getId(),
                    err
            );
        });
    }

    private boolean shouldLog(Member member) {
        return member.getRoles()
                .stream()
                .anyMatch(r -> REQUIRED_ROLES.contains(r.getId()));
    }

    private enum VoiceAction {
        JOIN,
        LEAVE,
        MOVE
    }
}