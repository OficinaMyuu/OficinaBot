package ofc.bot.listeners.discord.guilds.messages;

import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import ofc.bot.Main;
import ofc.bot.domain.entity.MessageTranscription;
import ofc.bot.domain.sqlite.repository.AppUserBanRepository;
import ofc.bot.domain.sqlite.repository.MessageTranscriptionRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@DiscordEventHandler
public class MessageTranscriptionsHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTranscriptionsHandler.class);
    private static final Emoji TRANSCRIPTION_EMOJI = Emoji.fromUnicode("\uD83C\uDF99\uFE0F"); // This is a microphone
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_CHUNK_LENGTH = Message.MAX_CONTENT_LENGTH - 5;
    private static final Set<String> VALID_EXTENSIONS;
    private final Set<Long> transcribingMessages = new HashSet<>();
    private final MessageTranscriptionRepository msgTrscptRepo;
    private final AppUserBanRepository appBanRepo;

    public MessageTranscriptionsHandler(MessageTranscriptionRepository msgTrscptRepo, AppUserBanRepository appBanRepo) {
        this.msgTrscptRepo = msgTrscptRepo;
        this.appBanRepo = appBanRepo;
    }

    // This method has the responsability of adding the reaction
    // if the given message has a voice-message (random .mp3 files are ignored).
    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();

        if (!msg.isFromGuild() || !msg.isVoiceMessage()) return;

        Message.Attachment audio = msg.getAttachments().getFirst();
        if (audio.getDuration() <= MessageTranscription.MAX_AUDIO_LENGTH_SECONDS && checkAudioFileExtension(audio)) {
            msg.addReaction(TRANSCRIPTION_EMOJI).queue();
        }
    }

    @Override
    public synchronized void onMessageReactionAdd(MessageReactionAddEvent e) {
        OpenAIClient openAI = Main.getOpenAI();
        EmojiUnion emoji = e.getEmoji();
        MessageChannel channel = e.getGuildChannel();
        long userId = e.getUserIdLong();
        long messageId = e.getMessageIdLong();

        if (!e.isFromGuild() || !emoji.equals(TRANSCRIPTION_EMOJI)) return;

        // Banned users cannot do anything
        if (appBanRepo.isBanned(userId)) return;

        // If we already have a transcription available (maybe it was just deleted in the past),
        // we run no checks, just send it immediately.
        // To avoid users requesting transcriptions successively and ending up flooding the chat,
        // we also include a cooldown of 6 hours.
        MessageTranscription transcription = msgTrscptRepo.findByMessageId(messageId);
        if (transcription != null) {
            if (!isInCooldown(transcription)) {
                channel.sendMessage(transcription.getTranscription())
                        .setMessageReference(messageId)
                        .queue();
            }
            return;
        }

        // If we are already transcribing the message, don't request it twice
        if (transcribingMessages.contains(messageId)) return;

        // If a new transcription is necessary, then we proceed to checking the user's permissions
        fetchResources(e, (member, msg) -> {
            if (!msg.isVoiceMessage() || !canTranscribe(member)) return;

            Message.Attachment audio = msg.getAttachments().getFirst();
            if (!validateAction(audio, member)) {
                msg.clearReactions(TRANSCRIPTION_EMOJI).queue();
                return;
            }

            sendTranscription(openAI, msg, audio, userId);
            msg.clearReactions(TRANSCRIPTION_EMOJI).queue();
        });
    }

    private boolean validateAction(Message.Attachment file, Member requester) {
        return file != null
                && requester != null
                && checkAudioFileExtension(file)
                && checkDurations(file, requester);
    }

    private boolean checkAudioFileExtension(Message.Attachment file) {
        return VALID_EXTENSIONS.contains(file.getFileExtension());
    }

    private boolean checkDurations(Message.Attachment file, Member requester) {
        double duration = file.getDuration();

        return requester.hasPermission(Permission.MANAGE_SERVER)
                ? duration <= MessageTranscription.LARGE_MAX_AUDIO_LENGTH_SECONDS
                : duration <= MessageTranscription.MAX_AUDIO_LENGTH_SECONDS;
    }

    private void fetchResources(MessageReactionAddEvent e, BiConsumer<Member, Message> callback) {
        e.retrieveMessage().queue(
                // Message success
                (msg) -> e.retrieveMember().queue(
                        // Member success
                        (member) -> callback.accept(member, msg),

                        // Member failure
                        (err) -> LOGGER.error("Failed to fetch member for transcription", err)
                ),

                // Message failure
                (err) -> LOGGER.error("Failed to fetch message for transcription", err)
        );
    }

    private void sendTranscribing(Message message, Consumer<Message> callback) {
        message.replyFormat("> %s transcrevendo áudio...", Bot.Emojis.LOADING.getFormatted())
                .queue(callback, (err) -> LOGGER.error("Failed to send transcription loading sign", err));
    }

    private File downloadAudio(Message.Attachment file) {
        try {
            String fileName = String.valueOf(System.nanoTime());
            return file.getProxy()
                    .downloadToFile(File.createTempFile(fileName, file.getFileExtension()))
                    .get();
        } catch (Exception e) {
            LOGGER.error("Failed downloading audio file for transcription", e);
            return null;
        }
    }

    private String generateTranscription(OpenAIClient openAI, File file) {
        try {
            TranscriptionCreateParams params = TranscriptionCreateParams.builder()
                    .model(AudioModel.GPT_4O_MINI_TRANSCRIBE)
                    .file(file.toPath())
                    .build();

            return openAI.audio()
                    .transcriptions()
                    .create(params)
                    .asTranscription()
                    .text();
        } catch (OpenAIInvalidDataException e) {
            LOGGER.error("Failed to create transcription", e);
        }
        return null;
    }

    private void sendTranscription(OpenAIClient openAI, Message origin, Message.Attachment file, long requesterId) {
        transcribingMessages.add(origin.getIdLong());
        sendTranscribing(origin, (output) -> {
            File tmpFile = downloadAudio(file);

            // We failed to download the file :/
            if (tmpFile == null) {
                output.editMessage("Failed :/").queue();
                return;
            }

            String tr = generateTranscription(openAI, tmpFile);
            if (tr == null) { // Failed to get the transcription
                output.editMessage("Failed :/").queue();
                return;
            }

            String fmtTr = String.format("## Transcrição\n> %s", tr);
            String[] messages = splitTranscription(fmtTr);

            persist(origin, tr, requesterId, file.getDuration());
            sendChained(output, messages);
            transcribingMessages.remove(origin.getIdLong());
        });
    }

    private void persist(Message origin, String transcription, long reqId, double audioLength) {
        long msgId = origin.getIdLong();
        long chanId = origin.getChannelIdLong();
        long now = Bot.unixNow();

        try {
            MessageTranscription tr = new MessageTranscription(msgId, chanId, reqId, audioLength, transcription, now);
            msgTrscptRepo.save(tr);
        } catch (Exception e) {
            LOGGER.error("Failed to persist transcription on {}/{}", chanId, msgId, e);
        }
    }

    private void sendChained(Message ref, String[] chunks) {
        MessageChannel chan = ref.getChannel();

        if (chunks.length == 1) {
            ref.replyFormat(chunks[0]).queue();
            return;
        }

        RestAction<Message> sendReq = ref.reply(chunks[0]);
        for (int i = 1; i < chunks.length; i++) {
            String msg = chunks[i];
            sendReq = sendReq.flatMap(s -> chan.sendMessage(msg));
        }
        sendReq.queue(null, (err) -> LOGGER.error("Failed to send chained transcrpition messages", err));
    }

    // In case the transcription overflows the Discord's message chars limit
    private String[] splitTranscription(String tr) {
        final String PREFIX = "> ";

        if (tr.length() <= Message.MAX_CONTENT_LENGTH) {
            return new String[]{tr};
        }

        List<String> result = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        for (String word : tr.split(" ")) {
            if (chunk.length() + word.length() + 1 > MAX_CHUNK_LENGTH) {
                // The word would overflow the current chunk and we don't want to cut it in half XD
                result.add(PREFIX + chunk.toString().trim());
                chunk = new StringBuilder();
            }
            chunk.append(word).append(" ");
        }

        if (!chunk.isEmpty()) {
            result.add(PREFIX + chunk.toString().trim());
        }

        result.set(0, result.getFirst().substring(PREFIX.length()));
        return result.toArray(new String[0]);
    }

    private boolean isInCooldown(MessageTranscription tr) {
        long now = Bot.unixNow();
        long creation = tr.getTimeCreated();
        return Bot.calcUnixPeriods(creation, now) < RESEND_COOLDOWN_SECONDS;
    }

    private boolean canTranscribe(Member member) {
        if (member.hasPermission(Permission.MANAGE_SERVER)) return true;

        long userId = member.getIdLong();
        int trCount = msgTrscptRepo.countDailyTranscriptionsByUserId(userId);

        return Staff.isStaff(member)
                ? trCount < MessageTranscription.DAILY_MAX_STAFF
                : trCount < MessageTranscription.DAILY_MAX;
    }

    static {
        VALID_EXTENSIONS = Set.of("flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm");
    }
}