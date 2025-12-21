package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.VoiceHeartbeat;
import ofc.bot.domain.tables.VoiceHeartbeatsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link VoiceHeartbeat} entity.
 */
public class VoiceHeartbeatRepository extends Repository<VoiceHeartbeat> {
    private static final VoiceHeartbeatsTable VOICE_HEARTBEATS = VoiceHeartbeatsTable.VOICE_HEARTBEATS;

    public VoiceHeartbeatRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<VoiceHeartbeat> getTable() {
        return VOICE_HEARTBEATS;
    }
}