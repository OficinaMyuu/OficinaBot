package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Reminder;
import ofc.bot.domain.tables.RemindersTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Repository for {@link Reminder} entity.
 */
public class ReminderRepository extends Repository<Reminder> {
    private static final RemindersTable REMINDERS = RemindersTable.REMINDERS;

    public ReminderRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    public int countByUserId(long userId, boolean whereExpired) {
        return ctx.fetchCount(
                REMINDERS,
                REMINDERS.USER_ID.eq(userId)
                        .and(REMINDERS.EXPIRED.eq(whereExpired))
        );
    }

    public Reminder findActiveById(int id) {
        return ctx.fetchOne(REMINDERS, REMINDERS.ID.eq(id).and(REMINDERS.EXPIRED.eq(false)));
    }

    @NotNull
    public List<Reminder> findAllActive() {
        return ctx.selectFrom(REMINDERS)
                .where(REMINDERS.EXPIRED.eq(false))
                .fetch();
    }

    @NotNull
    public List<Reminder> findAllActiveByUserId(long userId, String search, int limit) {
        return ctx.selectFrom(REMINDERS)
                .where(REMINDERS.EXPIRED.eq(false))
                .and(REMINDERS.USER_ID.eq(userId))
                .and(REMINDERS.MESSAGE.like('%' + search + '%'))
                .orderBy(REMINDERS.CREATED_AT.desc())
                .limit(limit)
                .fetch();
    }

    public boolean existsByExpressionAndUserId(long userId, String expression) {
        return ctx.fetchExists(
                REMINDERS,
                REMINDERS.USER_ID.eq(userId)
                        .and(REMINDERS.EXPRESSION.eq(expression))
                        .and(REMINDERS.EXPIRED.eq(false))
        );
    }

    @NotNull
    @Override
    public InitializableTable<Reminder> getTable() {
        return REMINDERS;
    }

    public List<Reminder> viewReminderByUserId(long userId, int offset, int limit) {
        return ctx.selectFrom(REMINDERS)
                .where(REMINDERS.USER_ID.eq(userId))
                .orderBy(REMINDERS.EXPIRED.asc(), REMINDERS.CREATED_AT.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public int countActiveByUserId(long userId) {
        return ctx.fetchCount(REMINDERS, REMINDERS.USER_ID.eq(userId).and(REMINDERS.EXPIRED.eq(false)));
    }

    public int countByUserId(long userId) {
        return ctx.fetchCount(REMINDERS, REMINDERS.USER_ID.eq(userId));
    }
}