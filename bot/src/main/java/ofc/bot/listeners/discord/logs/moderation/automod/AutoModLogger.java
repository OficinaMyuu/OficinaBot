package ofc.bot.listeners.discord.logs.moderation.automod;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.automod.AutoModResponse;
import net.dv8tion.jda.api.entities.automod.AutoModTriggerType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.automod.AutoModExecutionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataObject;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Channels;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

@DiscordEventHandler
public class AutoModLogger extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoModLogger.class);
    private static final Color RED_LOG = new Color(255, 50, 50);

    @Override
    public void onAutoModExecution(@NotNull AutoModExecutionEvent e) {
        TextChannel log = Channels.AUTOMOD_LOG.textChannel();
        if (log == null) {
            LOGGER.warn("Missed automod activity because log channel for id {} was not found",
                    Channels.AUTOMOD_LOG.fetchId());
            return;
        }

        MessageChannel channel = e.getChannel();
        Guild guild = e.getGuild();
        String keyword = e.getMatchedKeyword();
        String content = e.getContent();
        AutoModResponse response = e.getResponse();
        AutoModTriggerType type = e.getTriggerType();
        long userId = e.getUserIdLong();
        long msgId = e.getMessageIdLong();

        // We are retrieving an instance of the member to display their avatar in the chat
        // and get their timeout duration. For better logging design,
        // it's not expected for this "request" to fail because the member is
        // likely already cached and they do exist... right?
        guild.retrieveMemberById(userId).queue(member -> {

            long timeoutSeconds = resolveTimeout(e.getRawData());
            MessageEmbed embed = embed(channel, content, keyword, response, type, member, timeoutSeconds, msgId);

            log.sendMessageEmbeds(embed).queue();
        });
    }

    private MessageEmbed embed(
            MessageChannel chan,
            String message,
            String keyword,
            AutoModResponse resp,
            AutoModTriggerType type,
            Member member,
            long timeoutSeconds,
            long msgId
    ) {
        EmbedBuilder builder = new EmbedBuilder();
        Guild guild = member.getGuild();
        User user = member.getUser();
        AutoModResponse.Type respType = resp.getType();
        String respMessage = resp.getCustomMessage();
        String msgUrl = String.format(Message.JUMP_URL, guild.getId(), chan == null ? 0 : chan.getId(), msgId);

        builder
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setDescription("### Mensagem:\n" + message)
                .setColor(RED_LOG)
                .addField("🔑 Palavra-chave", keyword == null ? "" : keyword, true)
                .addField("📃 Tipo", type.name(), true)
                .addField("🎓 Medida Tomada", respType.name(), true)
                .setFooter(guild.getName(), guild.getIconUrl());

        if (chan != null)
            builder.addField("📖 Canal", chan.getAsMention(), true);

        if (timeoutSeconds != 0)
            builder.addField("🔇 Timeout", Bot.parsePeriod(timeoutSeconds), true);

        if (chan != null && msgId != 0)
            builder.addField("📑 Mensagem", msgUrl, true);

        if (respMessage != null)
            builder.addField("🤖 Resposta", respMessage, true);

        return builder.build();
    }

    /**
     * Retrieves the duration (in seconds) that a member has been timed out, based on the raw data received from Discord.
     * <p>
     * This method is used as a workaround for {@link AutoModResponse#getTimeoutDuration()}, which always returns
     * {@code null} due to a bug in JDA.
     * </p>
     * @param raw The raw JSON data received from Discord.
     * @return The amount of seconds the member has been timed out,
     * or {@code 0} if this is not a {@link AutoModResponse.Type#TIMEOUT} response.
     */
    private long resolveTimeout(DataObject raw) {

        if (raw == null)
            return 0;

        DataObject metadata = raw.getObject("d")
                .getObject("action")
                .optObject("metadata")
                .orElse(null);

        return metadata == null
                ? 0L
                : metadata.getUnsignedLong("duration_seconds", 0L);
    }
}