package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GiveawayEntry;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

public class GiveawayEntriesTable extends InitializableTable<GiveawayEntry> {
    public static final GiveawayEntriesTable GIVEAWAY_ENTRIES = new GiveawayEntriesTable();

    public final Field<String> GIVEAWAY_ID = newField("giveaway_id", CHAR.notNull());
    public final Field<Long> USER_ID       = newField("user_id", BIGINT.notNull());
    public final Field<Long> CREATED_AT    = newField("created_at", BIGINT.notNull());

    public GiveawayEntriesTable() {
        super("giveaway_entries");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(GIVEAWAY_ID, USER_ID)
                .columns(fields());
    }

    @NotNull
    @Override
    public Class<GiveawayEntry> getRecordType() {
        return GiveawayEntry.class;
    }
}
