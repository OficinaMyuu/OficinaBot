package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.RegisterData;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class RegistersTable extends InitializableTable<RegisterData> {
    public static final RegistersTable REGISTERS = new RegistersTable();

    public final Field<Integer> ID          = newField("id", SQLDataType.INTEGER.identity(true));
    public final Field<Long> TARGET_ID      = newField("target_id", SQLDataType.BIGINT.notNull());
    public final Field<Long> MODERATOR_ID   = newField("moderator_id", SQLDataType.BIGINT.notNull());
    public final Field<Integer> AGE         = newField("age", SQLDataType.INTEGER.notNull());
    public final Field<String> GENDER       = newField("gender", SQLDataType.VARCHAR(255).notNull());
    public final Field<String> DEVICE       = newField("device", SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> CREATED_AT     = newField("created_at", SQLDataType.BIGINT.notNull());

    public RegistersTable() {
        super("registers");
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }

    @NotNull
    @Override
    public Class<RegisterData> getRecordType() {
        return RegisterData.class;
    }
}
