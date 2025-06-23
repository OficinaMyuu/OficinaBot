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

    public long getTimeCreated() {
        return get(MESSAGES_TRANSCRIPTIONS.CREATED_AT);
    }


    private void checkPositiveAudio(double value) {
        if (value <= 0)
            throw new IllegalArgumentException("Audio Length cannot be less than, or equal to 0, provided: " + value);
    }
}