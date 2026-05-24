package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.RentStatus;
import ofc.bot.domain.tables.OficinaGroupsTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Stream;

/**
 * Repository for {@link OficinaGroup} entity.
 */
public class OficinaGroupRepository extends Repository<OficinaGroup> {
    private static final OficinaGroupsTable OFICINA_GROUPS = OficinaGroupsTable.OFICINA_GROUPS;

    public OficinaGroupRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<OficinaGroup> getTable() {
        return OFICINA_GROUPS;
    }

    public OficinaGroup findByOwnerId(long userId) {
        return ctx.selectFrom(OFICINA_GROUPS)
                .where(OFICINA_GROUPS.OWNER_ID.eq(userId))
                .fetchOne();
    }

    public OficinaGroup findById(int id) {
        return ctx.selectFrom(OFICINA_GROUPS)
                .where(OFICINA_GROUPS.ID.eq(id))
                .fetchOne();
    }

    public List<OficinaGroup> findByName(String search, int limit) {
        return ctx.selectFrom(OFICINA_GROUPS)
                .where(OFICINA_GROUPS.NAME.like('%' + search + '%'))
                .limit(limit)
                .fetch();
    }

    public boolean existsByOwnerId(long userId) {
        return ctx.fetchExists(OFICINA_GROUPS, OFICINA_GROUPS.OWNER_ID.eq(userId));
    }

    public List<OficinaGroup> findByRentStatus(RentStatus status) {
        return ctx.selectFrom(OFICINA_GROUPS)
                .where(OFICINA_GROUPS.RENT_STATUS.eq(status.name()))
                .fetch();
    }

    public List<OficinaGroup> findAllExcept(RentStatus... statuses) {
        List<String> statusList = Stream.of(statuses).map(RentStatus::name).toList();
        return ctx.selectFrom(OFICINA_GROUPS)
                .where(OFICINA_GROUPS.RENT_STATUS.notIn(statusList))
                .fetch();
    }

    public int updateGroupsStatus(RentStatus newStatus, RentStatus where) {
        long timestamp = Bot.unixNow();
        return ctx.update(OFICINA_GROUPS)
                .set(OFICINA_GROUPS.RENT_STATUS, newStatus.name())
                .set(OFICINA_GROUPS.UPDATED_AT, timestamp)
                .where(OFICINA_GROUPS.RENT_STATUS.eq(where.name()))
                .execute();
    }

    public void delete(OficinaGroup group) {
        ctx.deleteFrom(OFICINA_GROUPS)
                .where(OFICINA_GROUPS.ID.eq(group.getId()))
                .execute();
    }
}