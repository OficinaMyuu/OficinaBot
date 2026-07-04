package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.CommandHistory;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class CommandsHistoryTable extends InitializableTable<CommandHistory> {
    public static final CommandsHistoryTable COMMANDS_HISTORY = new CommandsHistoryTable();

    public final Field<Integer> ID             = newField("id",             SQLDataType.INTEGER.identity(true));
    public final Field<String> COMMAND_NAME    = newField("command_name",   SQLDataType.VARCHAR(255).notNull());
    public final Field<String> EXIT_STATUS     = newField("exit_status",    SQLDataType.VARCHAR(255).notNull());
    public final Field<Boolean> TICKS_COOLDOWN = newField("ticks_cooldown", SQLDataType.BOOLEAN.notNull());
    public final Field<Long> GUILD_ID          = newField("guild_id",       SQLDataType.BIGINT.notNull());
    public final Field<Long> USER_ID           = newField("user_id",        SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_AT        = newField("created_at",     SQLDataType.BIGINT.notNull());

    public CommandsHistoryTable() {
        super("commands_history");
    }

    @NotNull
    @Override
    public Class<CommandHistory> getRecordType() {
        return CommandHistory.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields());
    }
}