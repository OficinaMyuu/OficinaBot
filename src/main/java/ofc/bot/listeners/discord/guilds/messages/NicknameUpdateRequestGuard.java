package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.handlers.nick.NicknameEmojiPolicy;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@DiscordEventHandler
public class NicknameUpdateRequestGuard extends ListenerAdapter {
    public static final String NICK_UPDATE_CHANNEL_KEY = "channels.nick-update.id";
    private static final Logger LOGGER = LoggerFactory.getLogger(NicknameUpdateRequestGuard.class);
    private static final Emoji REJECTION_REACTION = Emoji.fromUnicode("❌");

    private final NicknameEmojiPolicy emojiPolicy;

    public NicknameUpdateRequestGuard(NicknameEmojiPolicy emojiPolicy) {
        this.emojiPolicy = Objects.requireNonNull(emojiPolicy);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()) {
            return;
        }

        Long nickUpdateChannelId = Bot.get(NICK_UPDATE_CHANNEL_KEY, Long::parseLong);
        if (nickUpdateChannelId == null || event.getChannel().getIdLong() != nickUpdateChannelId) {
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            return;
        }

        Message message = event.getMessage();
        String nickname = message.getContentRaw().strip();
        NicknameEmojiPolicy.NicknameEmojiReport report = emojiPolicy.inspect(member.getIdLong(), nickname);

        if (!report.hasTooManyEmojis() && !report.hasUnauthorizedStaffEmojis()) {
            return;
        }

        message.replyEmbeds(EmbedFactory.embedNicknameMessageRejected(member, nickname, report))
                .queue(null, error -> LOGGER.warn("Failed to reply to invalid nickname request {}", message.getId(), error));
        message.addReaction(REJECTION_REACTION)
                .queue(null, error -> LOGGER.warn("Failed to react to invalid nickname request {}", message.getId(), error));
    }
}
