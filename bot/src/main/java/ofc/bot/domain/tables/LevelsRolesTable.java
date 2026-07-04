package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.LevelRole;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

public class LevelsRolesTable extends InitializableTable<LevelRole> {
    public static final LevelsRolesTable LEVELS_ROLES = new LevelsRolesTable();

    public final Field<Integer> ID      = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Integer> LEVEL   = newField("level",      SQLDataType.INTEGER.notNull());
    public final Field<Long> ROLE_ID    = newField("role_id",    SQLDataType.BIGINT.notNull());
    public final Field<Long> CREATED_AT = newField("created_at", SQLDataType.BIGINT.notNull());

    public LevelsRolesTable() {
        super("levels_roles");
    }

    @NotNull
    @Override
    public Class<LevelRole> getRecordType() {
        return LevelRole.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .unique(LEVEL, ROLE_ID)
                .columns(fields());
    }
}
