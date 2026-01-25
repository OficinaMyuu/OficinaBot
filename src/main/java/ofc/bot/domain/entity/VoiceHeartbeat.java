package ofc.bot.domain.entity;

import ofc.bot.domain.tables.VoiceHeartbeatsTable;

public class VoiceHeartbeat extends OficinaRecord<VoiceHeartbeat> {
    private static final VoiceHeartbeatsTable VOICE_HEARTBEATS = VoiceHeartbeatsTable.VOICE_HEARTBEATS;

    public VoiceHeartbeat() {
        super(VOICE_HEARTBEATS);
    }

    public VoiceHeartbeat(
            long userId, long channelId, boolean isMuted,
            boolean isDeafened, boolean isVideo,
            boolean isStream, long timestamp
    ) {
        this();
        set(VOICE_HEARTBEATS.USER_ID, userId);
        set(VOICE_HEARTBEATS.CHANNEL_ID, channelId);
        set(VOICE_HEARTBEATS.IS_MUTED, isMuted);
        set(VOICE_HEARTBEATS.IS_DEAFENED, isDeafened);
        set(VOICE_HEARTBEATS.IS_VIDEO, isVideo);
        set(VOICE_HEARTBEATS.IS_STREAM, isStream);
        set(VOICE_HEARTBEATS.CREATED_AT, timestamp);
    }

    public int getId() {
        return get(VOICE_HEARTBEATS.ID);
    }

    public long getUserId() {
        return get(VOICE_HEARTBEATS.USER_ID);
    }

    public long getChannelId() {
        return get(VOICE_HEARTBEATS.CHANNEL_ID);
    }

    public boolean isMuted() {
        return get(VOICE_HEARTBEATS.IS_MUTED);
    }

    public boolean isDeafened() {
        return get(VOICE_HEARTBEATS.IS_DEAFENED);
    }

    public boolean isVideo() {
        return get(VOICE_HEARTBEATS.IS_VIDEO);
    }

    public boolean isStream() {
        return get(VOICE_HEARTBEATS.IS_STREAM);
    }

    public long getTimeCreated() {
        return get(VOICE_HEARTBEATS.CREATED_AT);
    }
}