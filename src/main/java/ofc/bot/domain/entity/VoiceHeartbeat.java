package ofc.bot.domain.entity;

import ofc.bot.domain.tables.VoiceHeartbeatsTable;

public class VoiceHeartbeat extends OficinaRecord<VoiceHeartbeat> {
    private static final VoiceHeartbeatsTable VOICE_HEARTBEATS = VoiceHeartbeatsTable.VOICE_HEARTBEATS;

    public VoiceHeartbeat() {
        super(VOICE_HEARTBEATS);
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

    public long getTimeCreated() {
        return get(VOICE_HEARTBEATS.CREATED_AT);
    }
}