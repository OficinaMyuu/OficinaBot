package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GroupBot;
import ofc.bot.domain.tables.GroupBotsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link GroupBot} entity.
 */
public class GroupBotRepository extends Repository<GroupBot> {
    private static final GroupBotsTable GROUP_BOTS = GroupBotsTable.GROUP_BOTS;

    public GroupBotRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<GroupBot> getTable() {
        return GROUP_BOTS;
    }

    public List<GroupBot> findByName(String name, int limit) {
        return ctx.selectFrom(GROUP_BOTS)
                .where(GROUP_BOTS.BOT_NAME.like('%' + name + '%'))
                .limit(limit)
                .fetch();
    }

    public GroupBot findById(int id) {
        return ctx.selectFrom(GROUP_BOTS)
                .where(GROUP_BOTS.ID.eq(id))
                .fetchOne();
    }
}