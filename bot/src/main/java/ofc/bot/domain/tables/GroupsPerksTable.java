package ofc.bot.domain.tables;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GroupPerk;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.SQLDataType;

import static ofc.bot.domain.tables.OficinaGroupsTable.OFICINA_GROUPS;
import static ofc.bot.domain.tables.UsersTable.USERS;

public class GroupsPerksTable extends InitializableTable<GroupPerk> {
    public static final GroupsPerksTable GROUPS_PERKS = new GroupsPerksTable();

    public final Field<Integer> ID         = newField("id",         SQLDataType.INTEGER.identity(true));
    public final Field<Integer> GROUP_ID   = newField("group_id",   SQLDataType.INTEGER.notNull());
    public final Field<Long> USER_ID       = newField("user_id",    SQLDataType.BIGINT.notNull());
    public final Field<String> ITEM        = newField("item",       SQLDataType.VARCHAR(255).notNull());
    public final Field<Integer> VALUE_PAID = newField("value_paid", SQLDataType.INTEGER.notNull());
    public final Field<String> CURRENCY    = newField("currency",   SQLDataType.VARCHAR(255).notNull());
    public final Field<Long> CREATED_AT    = newField("created_at", SQLDataType.BIGINT.notNull());

    public GroupsPerksTable() {
        super("groups_perks");
    }

    @NotNull
    @Override
    public Class<GroupPerk> getRecordType() {
        return GroupPerk.class;
    }

    @Override
    public Query getSchema(@NotNull DSLContext ctx) {
        return ctx.createTableIfNotExists(this)
                .primaryKey(ID)
                .columns(fields())
                .constraints(
                        foreignKey(GROUP_ID).references(OFICINA_GROUPS, GROUPS_PERKS.ID),
                        foreignKey(USER_ID).references(USERS, USERS.ID)
                );
    }
}