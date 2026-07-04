package ofc.bot.listeners.discord.guilds.messages;

import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.transcriptions.TranscriptionCreateParams;
import com.openai.models.moderations.Moderation;
import com.openai.models.moderations.ModerationCreateParams;
import com.openai.models.moderations.ModerationModel;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import ofc.bot.Main;
import ofc.bot.domain.entity.MessageTranscription;
import ofc.bot.domain.database.repository.AppUserBanRepository;
import ofc.bot.domain.database.repository.MessageTranscriptionRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@DiscordEventHandler
@SuppressWarnings("UnnecessaryUnicodeEscape")
public class MessageTranscriptionsHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageTranscriptionsHandler.class);
    private static final Emoji TRANSCRIPTION_EMOJI = Emoji.fromUnicode("\uD83C\uDF99\uFE0F"); // This is a microphone
    private static final String BANNED_USER_IDS_KEY = "messages.transcriptions.banned-user-ids";
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_CHUNK_LENGTH = Message.MAX_CONTENT_LENGTH - 5;
    private static final ExecutorService DATABASE_THREAD = Executors.newSingleThreadExecutor();
    private static final Set<String> VALID_EXTENSIONS;
    private final TranscriptionInFlight inFlightTranscriptions = new TranscriptionInFlight();
    private final MessageTranscriptionRepository transcriptionRepository;
    private final AppUserBanRepository appBanRepo;

    public MessageTranscriptionsHandler(MessageTranscriptionRepository transcriptionRepository, AppUserBanRepository appBanRepo) {
        this.transcriptionRepository = transcriptionRepository;
        this.appBanRepo = appBanRepo;
    }

    // Adds the transcription reaction to supported Discord voice messages.
    // Random audio uploads are ignored unless Discord marks them as voice messages.
    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();

        if (!msg.isFromGuild() || !msg.isVoiceMessage()) return;
        if (isTranscriptionBannedUser(msg.getAuthor().getIdLong())) return;

        Message.Attachment audio = findAudioAttachment(msg);
        if (audio != null && audio.getDuration() <= MessageTranscription.MAX_AUDIO_LENGTH_SECONDS) {
            msg.addReaction(TRANSCRIPTION_EMOJI).queue();
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent e) {
        OpenAIClient openAI = Main.getOpenAI();
        JDA api = Main.getApi();
        SelfUser self = api.getSelfUser();
        EmojiUnion emoji = e.getEmoji();
        MessageChannel channel = e.getGuildChannel();
        long userId = e.getUserIdLong();
        long messageId = e.getMessageIdLong();

        if (userId == self.getIdLong()) return;

        if (!e.isFromGuild() || !emoji.equals(TRANSCRIPTION_EMOJI)) return;

        // Banned users cannot do anything
        if (appBanRepo.isBanned(userId)) return;

        fetchResources(e, (member, msg) -> {
            if (isTranscriptionBannedUser(msg.getAuthor().getIdLong())) {
                msg.removeReaction(TRANSCRIPTION_EMOJI, member.getUser()).queue();
                return;
            }

            // If we already have a transcription available (maybe it was just deleted in the past),
            // we run no checks, just send it immediately.
            // Avoid flooding the chat when users repeatedly request an already persisted transcription.
            MessageTranscription transcription = transcriptionRepository.findByMessageId(messageId);
            if (transcription != null) {
                if (!isInCooldown(transcription)) {
                    channel.sendMessage(transcription.getTranscription())
                            .setAllowedMentions(List.of())
                            .setMessageReference(messageId)
                            .queue();
                }
                return;
            }

            if (!inFlightTranscriptions.tryStart(messageId)) return;

            // If a new transcription is necessary, then we proceed to checking the user's permissions
            if (!canTranscribe(member, msg)) {
                msg.removeReaction(TRANSCRIPTION_EMOJI, member.getUser()).queue();
                inFlightTranscriptions.finish(messageId);
                return;
            }

            Message.Attachment audio = findAudioAttachment(msg);
            if (audio == null || !checkDurations(audio, member)) {
                msg.clearReactions(TRANSCRIPTION_EMOJI).queue();
                inFlightTranscriptions.finish(messageId);
                return;
            }

            sendTranscription(openAI, msg, audio, userId);
            msg.clearReactions(TRANSCRIPTION_EMOJI).queue();
        }, () -> {});
    }

    private Message.Attachment findAudioAttachment(Message message) {
        if (!message.isVoiceMessage()) return null;

        return message.getAttachments().stream()
                .filter(this::checkAudioFileExtension)
                .findFirst()
                .orElse(null);
    }

    private boolean checkAudioFileExtension(Message.Attachment file) {
        return isValidAudioExtension(file.getFileExtension());
    }

    static boolean isValidAudioExtension(String extension) {
        if (extension == null) return false;
        return VALID_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    private boolean isTranscriptionBannedUser(long userId) {
        return isConfiguredUserId(Bot.getArray(BANNED_USER_IDS_KEY), userId);
    }

    static boolean isConfiguredUserId(String[] rawUserIds, long userId) {
        String expected = Long.toString(userId);
        for (String rawUserId : rawUserIds) {
            if (expected.equals(rawUserId.trim())) return true;
        }
        return false;
    }

    private boolean checkDurations(Message.Attachment file, Member requester) {
        double duration = file.getDuration();

        return requester.hasPermission(Permission.MANAGE_SERVER)
                ? duration <= MessageTranscription.LARGE_MAX_AUDIO_LENGTH_SECONDS
                : duration <= MessageTranscription.MAX_AUDIO_LENGTH_SECONDS;
    }

    private void fetchResources(MessageReactionAddEvent e, BiConsumer<Member, Message> callback, Runnable failureCallback) {
        e.retrieveMessage().queue(
                // Message success
                (msg) -> e.retrieveMember().queue(
                        // Member success
                        (member) -> callback.accept(member, msg),

                        // Member failure
                        (err) -> {
                            failureCallback.run();
                            LOGGER.error("Failed to fetch member for transcription", err);
                        }
                ),

                // Message failure
                (err) -> {
                    failureCallback.run();
                    LOGGER.error("Failed to fetch message for transcription", err);
                }
        );
    }

    private void sendTranscribing(Message message, Consumer<Message> callback, Runnable failureCallback) {
        message.replyFormat("> %s transcrevendo áudio...", Bot.Emojis.LOADING.getFormatted())
                .mentionRepliedUser(false)
                .queue(callback, (err) -> {
                    failureCallback.run();
                    LOGGER.error("Failed to send transcription loading sign", err);
                });
    }

    private File downloadAudio(Message.Attachment file) {
        try {
            String fileName = String.valueOf(System.nanoTime());
            String fileExt = String.format(".%s", file.getFileExtension());

            return file.getProxy()
                    .downloadToFile(File.createTempFile(fileName, fileExt))
                    .get();
        } catch (Exception e) {
            LOGGER.error("Failed downloading audio file for transcription", e);
            return null;
        }
    }

    private String generateTranscription(OpenAIClient openAI, File file) {
        try {
            TranscriptionCreateParams params = TranscriptionCreateParams.builder()
                    .model(AudioModel.GPT_4O_TRANSCRIBE)
                    .language("pt")
                    .file(file.toPath())
                    .build();

            String result = openAI.audio()
                    .transcriptions()
                    .create(params)
                    .asTranscription()
                    .text();

            file.delete();
            return result;
        } catch (OpenAIInvalidDataException e) {
            LOGGER.error("Failed to create transcription", e);
        }
        return null;
    }

    private void sendTranscription(OpenAIClient openAI, Message origin, Message.Attachment file, long requesterId) {
        long messageId = origin.getIdLong();
        sendTranscribing(origin, (output) -> {
            File tmpFile = downloadAudio(file);

            // We failed to download the file :/
            if (tmpFile == null) {
                output.editMessage("Failed :/").queue();
                inFlightTranscriptions.finish(messageId);
                return;
            }

            String tr = generateTranscription(openAI, tmpFile);
            if (tr == null) { // Failed to get the transcription
                output.editMessage("Failed :/").queue();
                inFlightTranscriptions.finish(messageId);
                return;
            }

            String fmtTr = String.format("## Transcrição\n> %s", tr);
            String[] messages = splitTranscription(fmtTr);

            // Just to speed up stuff, as we are going to be making more requests
            // and we don't want to make our users keep waiting even more
            DATABASE_THREAD.execute(() -> persist(origin, openAI, tr, requesterId, file));

            sendChained(output, messages, () -> inFlightTranscriptions.finish(messageId));
        }, () -> inFlightTranscriptions.finish(messageId));
    }

    private void persist(Message origin, OpenAIClient openAI, String transcription, long reqId, Message.Attachment file) {
        String fileExtension = file.getFileExtension();
        double audioLength = file.getDuration();
        long msgId = origin.getIdLong();
        long chanId = origin.getChannelIdLong();
        long now = Bot.unixNow();

        try {
            MessageTranscription tr = new MessageTranscription(msgId, chanId, reqId, audioLength, transcription, now);

            // This is a best-effort operation, if the request fail, well, sad day for us ^^
            applyModerations(tr, openAI, transcription);
            tr.setFileExtension(fileExtension);

            transcriptionRepository.save(tr);
        } catch (Exception e) {
            LOGGER.error("Failed to persist transcription on {}/{}", chanId, msgId, e);
        }
    }

    private void applyModerations(MessageTranscription rec, OpenAIClient openAI, String tr) {
        try {
            Moderation moderation = getModeration(openAI, tr);
            Moderation.CategoryScores scores = moderation.categoryScores();
            boolean flagged = moderation.flagged();
            double sexual = avg(scores.sexual(), scores.sexualMinors());
            double hate = avg(scores.hate(), scores.hateThreatening());
            double illicit = avg(scores.illicit(), scores.illicitViolent());
            double selfHarm = avg(scores.selfHarm(), scores.selfHarmIntent(), scores.selfHarmInstructions());
            double violence = avg(scores.violence(), scores.violenceGraphic());

            rec.setHarmful(flagged)
                    .setSexualScore(sexual)
                    .setHateScore(hate)
                    .setIllicitScore(illicit)
                    .setSelfHarmScore(selfHarm)
                    .setViolenceScore(violence);
        } catch (Exception e) {
            LOGGER.error("Failed to apply moderation", e);
        }
    }

    private double avg(double... values) {
        return Arrays.stream(values).sum() / values.length;
    }

    private Moderation getModeration(OpenAIClient openAI, String transcription) {
        ModerationCreateParams params = ModerationCreateParams.builder()
                .model(ModerationModel.OMNI_MODERATION_LATEST)
                .input(transcription)
                .build();

        List<Moderation> results = openAI.moderations()
                .create(params)
                .results();
        int resultCount = results.size();

        if (resultCount != 1) {
            LOGGER.warn("Unexpected: We got more results than inputs provided ({}), how??? " +
                    "Ignoring it and using only the first one", resultCount);
        }

        // It is expected to never get an empty array as response, it makes absolutely no sense
        return results.getFirst();
    }

    private void sendChained(Message ref, String[] chunks, Runnable completionCallback) {
        MessageChannel chan = ref.getChannel();

        if (chunks.length == 1) {
            ref.editMessage(chunks[0]).queue(
                    (ignored) -> completionCallback.run(),
                    (err) -> {
                        completionCallback.run();
                        LOGGER.error("Failed to send transcription message", err);
                    }
            );
            return;
        }

        RestAction<Message> sendReq = ref.editMessage(chunks[0]).setAllowedMentions(List.of());
        for (int i = 1; i < chunks.length; i++) {
            String msg = chunks[i];
            sendReq = sendReq.flatMap(s -> chan.sendMessage(msg));
        }
        sendReq.queue(
                (ignored) -> completionCallback.run(),
                (err) -> {
                    completionCallback.run();
                    LOGGER.error("Failed to send chained transcription messages", err);
                }
        );
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
        return Bot.distance(creation, now) < RESEND_COOLDOWN_SECONDS;
    }

    private boolean canTranscribe(Member requester, Message msg) {
        if (requester.hasPermission(Permission.MANAGE_SERVER)) return true;

        long userId = requester.getIdLong();
        if (userId == msg.getAuthor().getIdLong()) return false;

        int trCount = transcriptionRepository.countDailyTranscriptionsByUserId(userId);
        return Staff.isStaff(requester)
                ? trCount < MessageTranscription.DAILY_MAX_STAFF
                : trCount < MessageTranscription.DAILY_MAX;
    }

    static final class TranscriptionInFlight {
        private final Set<Long> messageIds = ConcurrentHashMap.newKeySet();

        boolean tryStart(long messageId) {
            return messageIds.add(messageId);
        }

        void finish(long messageId) {
            messageIds.remove(messageId);
        }

        boolean isRunning(long messageId) {
            return messageIds.contains(messageId);
        }
    }

    static {
        VALID_EXTENSIONS = Set.of("flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm");
    }
}
