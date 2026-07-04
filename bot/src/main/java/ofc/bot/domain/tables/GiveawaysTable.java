package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Giveaway;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class GiveawaysTable extends InitializableTable<Giveaway> {
    public static final GiveawaysTable GIVEAWAYS = new GiveawaysTable();

    public final Field<String> GIVEAWAY_ID               = newField("giveaway_id", SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> GUILD_ID                    = newField("guild_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> CHANNEL_ID                  = newField("channel_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> MESSAGE_ID                  = newField("message_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> HOST_ID                     = newField("host_id", SQLDataType.BIGINT.notNull());
    public final Field<String> STATUS                    = newField("status", SQLDataType.VARCHAR(255).notNull());
    public final Field<String> PRIZE_TYPE                = newField("prize_type", SQLDataType.VARCHAR(255).notNull());
    public final Field<String> TITLE                     = newField("title", SQLDataType.VARCHAR(255).notNull());
    public final Field<String> DESCRIPTION               = newField("description", SQLDataType.VARCHAR(255));
    public final Field<Integer> WINNER_COUNT             = newField("winner_count", SQLDataType.INTEGER.notNull());
    public final Field<Long> ENDS_AT                     = newField("ends_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> ENDED_AT                    = newField("ended_at", SQLDataType.BIGINT);
    public final Field<Long> REQUIRED_VOICE_CHANNEL_ID   = newField("required_voice_channel_id", SQLDataType.BIGINT);
    public final Field<Long> MONEY_AMOUNT                = newField("money_amount", SQLDataType.BIGINT);
    public final Field<Long> COLOR_ROLE_DURATION_SECONDS = newField("color_role_duration_seconds", SQLDataType.BIGINT);
    public final Field<Long> CREATED_AT                  = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT                  = newField("updated_at", SQLDataType.BIGINT.notNull());

    public GiveawaysTable() {
        super("giveaways");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(GIVEAWAY_ID)
                .columns(fields())
                .unique(MESSAGE_ID);
    }

    @NotNull
    @Override
    public Class<Giveaway> getRecordType() {
        return Giveaway.class;
    }
}
