package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GiveawayWinner;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;

public class GiveawayWinnersTable extends InitializableTable<GiveawayWinner> {
    public static final GiveawayWinnersTable GIVEAWAY_WINNERS = new GiveawayWinnersTable();

    public final Field<Integer> ID             = newField("id", INT.identity(true));
    public final Field<String> GIVEAWAY_ID    = newField("giveaway_id", CHAR.notNull());
    public final Field<Long> USER_ID          = newField("user_id", BIGINT.notNull());
    public final Field<String> STATUS         = newField("status", CHAR.notNull());
    public final Field<String> CURRENCY       = newField("currency", CHAR);
    public final Field<Long> COLOR_ROLE_ID    = newField("color_role_id", BIGINT);
    public final Field<Long> CLAIMED_AT       = newField("claimed_at", BIGINT);
    public final Field<Long> CREATED_AT       = newField("created_at", BIGINT.notNull());
    public final Field<Long> UPDATED_AT       = newField("updated_at", BIGINT.notNull());

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
