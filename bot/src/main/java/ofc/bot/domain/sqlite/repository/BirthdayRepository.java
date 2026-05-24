package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.Birthday;
import ofc.bot.domain.tables.BirthdaysTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.time.Month;
import java.util.List;

import static org.jooq.impl.DSL.*;

/**
 * Repository for {@link Birthday} entity.
 */
public class BirthdayRepository extends Repository<Birthday> {
    private static final BirthdaysTable BIRTHDAYS = BirthdaysTable.BIRTHDAYS;

    public BirthdayRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<Birthday> getTable() {
        return BIRTHDAYS;
    }

    public List<Birthday> findByCurrentMonth() {
        return ctx.selectFrom(BIRTHDAYS)
                .where(day(BIRTHDAYS.BIRTHDAY).eq(day(currentDate()))
                        .and(month(BIRTHDAYS.BIRTHDAY).eq(month(currentDate())))
                )
                .fetch();
    }

    public Birthday findByUserId(long userId) {
        return ctx.selectFrom(BIRTHDAYS)
                .where(BIRTHDAYS.USER_ID.eq(userId))
                .fetchOne();
    }

    public List<Birthday> findByMonth(Month month) {
        return ctx.selectFrom(BIRTHDAYS)
                .where(month(BIRTHDAYS.BIRTHDAY).eq(month.getValue()))
                .groupBy(BIRTHDAYS.USER_ID)
                .orderBy(day(BIRTHDAYS.BIRTHDAY).asc())
                .fetch();
    }

    public void delete(Birthday birthday) {
        ctx.executeDelete(birthday, BIRTHDAYS.USER_ID.eq(birthday.getUserId()));
    }
}