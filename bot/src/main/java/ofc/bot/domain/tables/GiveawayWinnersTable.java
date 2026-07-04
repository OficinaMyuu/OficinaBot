package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GiveawayWinner;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class GiveawayWinnersTable extends InitializableTable<GiveawayWinner> {
    public static final GiveawayWinnersTable GIVEAWAY_WINNERS = new GiveawayWinnersTable();

    public final Field<Integer> ID             = newField("id", SQLDataType.INTEGER.identity(true));
    public final Field<String> GIVEAWAY_ID    = newField("giveaway_id", SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> USER_ID          = newField("user_id", SQLDataType.BIGINT.notNull());
    public final Field<String> STATUS         = newField("status", SQLDataType.VARCHAR(255).notNull());
    public final Field<String> CURRENCY       = newField("currency", SQLDataType.VARCHAR(255));
    public final Field<Long> COLOR_ROLE_ID    = newField("color_role_id", SQLDataType.BIGINT);
    public final Field<Long> CLAIMED_AT       = newField("claimed_at", SQLDataType.BIGINT);
    public final Field<Long> CREATED_AT       = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT       = newField("updated_at", SQLDataType.BIGINT.notNull());

    public GiveawayWinnersTable() {
        super("giveaway_winners");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(GIVEAWAY_ID, USER_ID, CREATED_AT);
    }

    @NotNull
    @Override
    public Class<GiveawayWinner> getRecordType() {
        return GiveawayWinner.class;
    }
}
