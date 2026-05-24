package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GroupPerk;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.tables.GroupsPerksTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import static org.jooq.impl.DSL.sum;

/**
 * Repository for {@link GroupPerk} entity.
 */
public class GroupPerkRepository extends Repository<GroupPerk> {
    private static final GroupsPerksTable GROUP_PERKS = GroupsPerksTable.GROUPS_PERKS;

    public GroupPerkRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    public int countFree(int groupId, StoreItemType item) {
        return ctx.fetchCount(GROUP_PERKS,
                GROUP_PERKS.GROUP_ID.eq(groupId)
                        .and(GROUP_PERKS.ITEM.eq(item.name()))
                        .and(GROUP_PERKS.VALUE_PAID.eq(0))
        );
    }

    @SuppressWarnings("DataFlowIssue")
    public int sumPerksByGroupId(int groupId) {
        return ctx.select(sum(GROUP_PERKS.VALUE_PAID))
                .from(GROUP_PERKS)
                .where(GROUP_PERKS.GROUP_ID.eq(groupId))
                .fetchOneInto(int.class);
    }

    @NotNull
    @Override
    public InitializableTable<GroupPerk> getTable() {
        return GROUP_PERKS;
    }
}