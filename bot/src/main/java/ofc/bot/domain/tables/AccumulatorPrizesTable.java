package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.AccumulatorPrize;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class AccumulatorPrizesTable extends InitializableTable<AccumulatorPrize> {
    public static final AccumulatorPrizesTable ACCUMULATOR_PRIZES = new AccumulatorPrizesTable();

    public final Field<Integer> ID                   = newField("id", SQLDataType.INTEGER.identity(true));
    public final Field<Long> GUILD_ID                = newField("guild_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> TARGET_ID               = newField("target_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_BY              = newField("created_by", SQLDataType.BIGINT.notNull());
    public final Field<String> TYPE                  = newField("type", SQLDataType.VARCHAR(255).notNull());
    public final Field<String> STATUS                = newField("status", SQLDataType.VARCHAR(255).notNull());
    public final Field<Integer> AMOUNT               = newField("amount", SQLDataType.INTEGER);
    public final Field<String> CURRENCY              = newField("currency", SQLDataType.VARCHAR(255));
    public final Field<Long> COLOR_ROLE_ID           = newField("color_role_id", SQLDataType.BIGINT);
    public final Field<Long> COLOR_DURATION_SECONDS  = newField("color_duration_seconds", SQLDataType.BIGINT);
    public final Field<Long> APPROVED_BY             = newField("approved_by", SQLDataType.BIGINT);
    public final Field<Long> APPROVED_AT             = newField("approved_at", SQLDataType.BIGINT);
    public final Field<Long> REJECTED_BY             = newField("rejected_by", SQLDataType.BIGINT);
    public final Field<Long> REJECTED_AT             = newField("rejected_at", SQLDataType.BIGINT);
    public final Field<String> LAST_ERROR            = newField("last_error", SQLDataType.VARCHAR(255));
    public final Field<Long> CREATED_AT              = newField("created_at", SQLDataType.BIGINT.notNull());
    public final Field<Long> UPDATED_AT              = newField("updated_at", SQLDataType.BIGINT.notNull());

    public AccumulatorPrizesTable() {
        super("accumulator_prizes");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }

    @NotNull
    @Override
    public Class<AccumulatorPrize> getRecordType() {
        return AccumulatorPrize.class;
    }
}
