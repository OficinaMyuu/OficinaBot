package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.NicknameUpdateRequest;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class NicknameUpdateRequestsTable extends InitializableTable<NicknameUpdateRequest> {
    public static final NicknameUpdateRequestsTable NICKNAME_UPDATE_REQUESTS = new NicknameUpdateRequestsTable();

    public final Field<String> REQUEST_ID             = newField("request_id",               SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> GUILD_ID                 = newField("guild_id",                 SQLDataType.BIGINT.notNull());
    public final Field<Long> CHANNEL_ID               = newField("channel_id",               SQLDataType.BIGINT.notNull());
    public final Field<Long> MESSAGE_ID               = newField("message_id",               SQLDataType.BIGINT.notNull());
    public final Field<Long> TARGET_USER_ID           = newField("target_user_id",           SQLDataType.BIGINT.notNull());
    public final Field<Long> SUBMITTED_BY_ID          = newField("submitted_by_id",          SQLDataType.BIGINT.notNull());
    public final Field<String> NICKNAME               = newField("nickname",                 SQLDataType.VARCHAR(255).notNull());
    public final Field<String> APPROVE_BUTTON_ID      = newField("approve_button_id",        SQLDataType.VARCHAR(255).notNull());
    public final Field<String> REJECT_BUTTON_ID       = newField("reject_button_id",         SQLDataType.VARCHAR(255).notNull());
    public final Field<String> STATUS                 = newField("status",                   SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> DECISION_AUTHOR_ID       = newField("decision_author_id",       SQLDataType.BIGINT);
    public final Field<Long> DECIDED_AT               = newField("decided_at",               SQLDataType.BIGINT);
    public final Field<String> EMOJI_APPROVAL_SUMMARY = newField("emoji_approval_summary",   SQLDataType.VARCHAR(255));
    public final Field<String> UNAUTHORIZED_SUMMARY   = newField("unauthorized_summary",     SQLDataType.VARCHAR(255));
    public final Field<Long> CREATED_AT               = newField("created_at",               SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT               = newField("updated_at",               SQLDataType.BIGINT.notNull());

    public NicknameUpdateRequestsTable() {
        super("nickname_update_requests");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(REQUEST_ID)
                .columns(fields())
                .unique(MESSAGE_ID)
                .unique(APPROVE_BUTTON_ID)
                .unique(REJECT_BUTTON_ID);
    }

    @NotNull
    @Override
    public Class<NicknameUpdateRequest> getRecordType() {
        return NicknameUpdateRequest.class;
    }
}
