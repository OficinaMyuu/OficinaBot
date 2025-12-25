package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.ColorRoleState;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;

public class ColorRolesStateTable extends InitializableTable<ColorRoleState> {
    public static final ColorRolesStateTable COLOR_ROLES_STATES = new ColorRolesStateTable();

    public final Field<Integer> ID         = newField("id",         INT.identity(true));
    public final Field<Integer> VALUE_PAID = newField("value_paid", INT.notNull());
    public final Field<String> CURRENCY    = newField("currency",   CHAR.notNull());
    public final Field<Long> USER_ID       = newField("user_id",    BIGINT.notNull());
    public final Field<Long> GUILD_ID      = newField("guild_id",   BIGINT.notNull());
    public final Field<Long> ROLE_ID       = newField("role_id",    BIGINT.notNull());
    public final Field<Long> CREATED_AT    = newField("created_at", BIGINT.notNull());
    public final Field<Long> UPDATED_AT    = newField("updated_at", BIGINT.notNull());

    public ColorRolesStateTable() {
        super("color_roles_state");
    }

    @NotNull
    @Override
    public Class<ColorRoleState> getRecordType() {
        return ColorRoleState.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(ROLE_ID, GUILD_ID, CREATED_AT);
    }
}