package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.VoiceHeartbeat;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

import static ofc.bot.domain.tables.UsersTable.USERS;

public class VoiceHeartbeatsTable extends InitializableTable<VoiceHeartbeat> {
    public static final VoiceHeartbeatsTable VOICE_HEARTBEATS = new VoiceHeartbeatsTable();

    public final Field<Integer> ID          = newField("id",          INT.identity(true));
    public final Field<Long> USER_ID        = newField("user_id",     BIGINT.notNull());
    public final Field<Long> CHANNEL_ID     = newField("channel_id",  BIGINT.notNull());
    public final Field<Boolean> IS_MUTED    = newField("is_muted",    BOOL.notNull());
    public final Field<Boolean> IS_DEAFENED = newField("is_deafened", BOOL.notNull());
    public final Field<Long> CREATED_AT     = newField("created_at",  BIGINT.notNull());

    public VoiceHeartbeatsTable() {
        super("voice_heartbeats");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .constraint(
                        foreignKey(USER_ID).references(USERS, USERS.ID)
                );
    }

    @NotNull
    @Override
    public Class<VoiceHeartbeat> getRecordType() {
        return VoiceHeartbeat.class;
    }
}