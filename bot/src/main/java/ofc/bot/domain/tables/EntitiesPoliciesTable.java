package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.EntityPolicy;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;

public class EntitiesPoliciesTable extends InitializableTable<EntityPolicy> {
    public static final EntitiesPoliciesTable ENTITIES_POLICIES = new EntitiesPoliciesTable();

    public final Field<Integer> ID           = newField("id",            INT.identity(true));
    public final Field<String> RESOURCE      = newField("resource",      CHAR.notNull());
    public final Field<String> RESOURCE_TYPE = newField("resource_type", CHAR.notNull());
    public final Field<String> POLICY_TYPE   = newField("policy_type",   CHAR.notNull());
    public final Field<Long> CREATED_AT      = newField("created_at",    BIGINT.notNull());

    public EntitiesPoliciesTable() {
        super("entities_policies");
    }

    @NotNull
    @Override
    public Class<EntityPolicy> getRecordType() {
        return EntityPolicy.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) throws DataAccessException {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .unique(RESOURCE, POLICY_TYPE);
    }
}