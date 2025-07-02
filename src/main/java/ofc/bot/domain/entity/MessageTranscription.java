package ofc.bot.domain.entity;

import ofc.bot.domain.tables.MessagesTranscriptionsTable;
import org.jetbrains.annotations.NotNull;

public class MessageTranscription extends OficinaRecord<MessageTranscription> {
    public static final int DAILY_MAX = 3;
    public static final int DAILY_MAX_STAFF = 5;
    public static final int MAX_AUDIO_LENGTH_SECONDS = 2 * 60;
    public static final int LARGE_MAX_AUDIO_LENGTH_SECONDS = 10 * 60;
    private static final MessagesTranscriptionsTable MESSAGES_TRANSCRIPTIONS = MessagesTranscriptionsTable.MESSAGES_TRANSCRIPTIONS;

    public MessageTranscription() {
        super(MESSAGES_TRANSCRIPTIONS);
    }

    public MessageTranscription(long messageId, long channelId, long requesterId, double audioLength,
                                @NotNull String transcription, long createdAt) {
        this();
        checkPositiveAudio(audioLength);
        set(MESSAGES_TRANSCRIPTIONS.MESSAGE_ID, messageId);
        set(MESSAGES_TRANSCRIPTIONS.CHANNEL_ID, channelId);
        set(MESSAGES_TRANSCRIPTIONS.REQUESTER_ID, requesterId);
        set(MESSAGES_TRANSCRIPTIONS.AUDIO_LENGTH, audioLength);
        set(MESSAGES_TRANSCRIPTIONS.TRANSCRIPTION, transcription);
        set(MESSAGES_TRANSCRIPTIONS.CREATED_AT, createdAt);
    }

    public int getId() {
        return get(MESSAGES_TRANSCRIPTIONS.ID);
    }

    public long getMessageId() {
        return get(MESSAGES_TRANSCRIPTIONS.MESSAGE_ID);
    }

    public long getChannelId() {
        return get(MESSAGES_TRANSCRIPTIONS.CHANNEL_ID);
    }

    public long getRequesterId() {
        return get(MESSAGES_TRANSCRIPTIONS.REQUESTER_ID);
    }

    public double getAudioLength() {
        return get(MESSAGES_TRANSCRIPTIONS.AUDIO_LENGTH);
    }

    public String getTranscription() {
        return get(MESSAGES_TRANSCRIPTIONS.TRANSCRIPTION);
    }

    public String getFileExtension() {
        return get(MESSAGES_TRANSCRIPTIONS.FILE_EXTENSION);
    }

    /**
     * Checks if this transcription is considered harmful or offensive, according to
     * <a href="https://platform.openai.com/docs/guides/moderation">OpenAI's Moderation</a>
     * endpoint.
     * <p>
     * This method will also return {@code false} if the bot was not able to
     * resolve the moderation status in time.
     *
     * @return {@code true} if the transcription is harmful/offensive, {@code false} otherwise.
     */
    public boolean isHarmful() {
        Boolean val = get(MESSAGES_TRANSCRIPTIONS.IS_HARMFUL);
        return val != null && val;
    }

    public double getSexualScore() {
        Double score = get(MESSAGES_TRANSCRIPTIONS.SEXUAL_SCORE);
        return safeDouble(score);
    }

    public double getHateScore() {
        Double score = get(MESSAGES_TRANSCRIPTIONS.HATE_SCORE);
        return safeDouble(score);
    }

    public double getIllicitScore() {
        Double score = get(MESSAGES_TRANSCRIPTIONS.ILLICIT_SCORE);
        return safeDouble(score);
    }

    public double getSelfHarmScore() {
        Double score = get(MESSAGES_TRANSCRIPTIONS.SELF_HARM_SCORE);
        return safeDouble(score);
    }

    public double getViolenceScore() {
        Double score = get(MESSAGES_TRANSCRIPTIONS.VIOLENCE_SCORE);
        return safeDouble(score);
    }

    public long getTimeCreated() {
        return get(MESSAGES_TRANSCRIPTIONS.CREATED_AT);
    }

    public MessageTranscription setMessageId(long messageId) {
        set(MESSAGES_TRANSCRIPTIONS.MESSAGE_ID, messageId);
        return this;
    }

    public MessageTranscription setChannelId(long channelId) {
        set(MESSAGES_TRANSCRIPTIONS.CHANNEL_ID, channelId);
        return this;
    }

    public MessageTranscription setRequesterId(long requesterId) {
        set(MESSAGES_TRANSCRIPTIONS.REQUESTER_ID, requesterId);
        return this;
    }

    public MessageTranscription setAudioLength(double audioLength) {
        set(MESSAGES_TRANSCRIPTIONS.AUDIO_LENGTH, audioLength);
        return this;
    }

    public MessageTranscription setTranscription(String transcription) {
        set(MESSAGES_TRANSCRIPTIONS.TRANSCRIPTION, transcription);
        return this;
    }

    public MessageTranscription setFileExtension(String fileExtension) {
        set(MESSAGES_TRANSCRIPTIONS.FILE_EXTENSION, fileExtension);
        return this;
    }

    public MessageTranscription setHarmful(boolean isHarmful) {
        set(MESSAGES_TRANSCRIPTIONS.IS_HARMFUL, isHarmful);
        return this;
    }

    public MessageTranscription setSexualScore(double score) {
        set(MESSAGES_TRANSCRIPTIONS.SEXUAL_SCORE, score);
        return this;
    }

    public MessageTranscription setHateScore(double score) {
        set(MESSAGES_TRANSCRIPTIONS.HATE_SCORE, score);
        return this;
    }

    public MessageTranscription setIllicitScore(double score) {
        set(MESSAGES_TRANSCRIPTIONS.ILLICIT_SCORE, score);
        return this;
    }

    public MessageTranscription setSelfHarmScore(double score) {
        set(MESSAGES_TRANSCRIPTIONS.SELF_HARM_SCORE, score);
        return this;
    }

    public MessageTranscription setViolenceScore(double score) {
        set(MESSAGES_TRANSCRIPTIONS.VIOLENCE_SCORE, score);
        return this;
    }

    private double safeDouble(Double val) {
        return val == null ? 0 : val;
    }

    private void checkPositiveAudio(double value) {
        if (value <= 0)
            throw new IllegalArgumentException("Audio Length cannot be less than, or equal to 0, provided: " + value);
    }
}