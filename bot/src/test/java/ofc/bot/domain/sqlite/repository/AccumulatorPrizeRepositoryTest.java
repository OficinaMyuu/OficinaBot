package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeStatus;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.tables.AccumulatorPrizesTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccumulatorPrizeRepositoryTest {
    @Test
    void shouldSortPendingByTypePriorityThenMoneyAmountDescending() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            AccumulatorPrizeRepository repository = new AccumulatorPrizeRepository(setup(connection));

            repository.save(money(10L, 100, 100L));
            repository.save(color(20L, 200L));
            repository.save(money(30L, 500, 300L));

            List<AccumulatorPrize> pending = repository.findPending(1L, 0, 10);

            assertEquals(3, pending.size());
            assertEquals(AccumulatorPrizeType.COLOR_ROLE, pending.get(0).getType());
            assertEquals(500, pending.get(1).getAmount());
            assertEquals(100, pending.get(2).getAmount());
        }
    }

    @Test
    void shouldConfigureRejectAndMarkPaidWithoutDeletingRows() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            AccumulatorPrizeRepository repository = new AccumulatorPrizeRepository(setup(connection));

            repository.save(money(10L, 100, 100L));
            repository.save(money(20L, 200, 200L));
            List<AccumulatorPrize> pending = repository.findAllPending(1L);
            int rejectedId = pending.getFirst().getId();
            int paidId = pending.get(1).getId();

            assertEquals(CurrencyType.UNBELIEVABOAT, repository.findById(1L, paidId).getCurrency());
            assertTrue(repository.updateCurrency(1L, paidId, CurrencyType.OFICINA, 300L));
            assertTrue(repository.reject(1L, rejectedId, 99L, 400L));
            assertEquals(1, repository.markPaid(1L, List.of(paidId), 98L, 500L));

            assertEquals(0, repository.countPending(1L));
            assertEquals(AccumulatorPrizeStatus.REJECTED, repository.findById(1L, rejectedId).getStatus());
            assertEquals(99L, repository.findById(1L, rejectedId).getRejectedBy());
            assertEquals(AccumulatorPrizeStatus.PAID, repository.findById(1L, paidId).getStatus());
            assertEquals(98L, repository.findById(1L, paidId).getApprovedBy());
            assertEquals(2, repository.countAll());
        }
    }

    @Test
    void shouldFindPendingTargetIdsOnly() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            AccumulatorPrizeRepository repository = new AccumulatorPrizeRepository(setup(connection));

            repository.save(money(10L, 100, 100L));
            repository.save(money(20L, 200, 200L));
            int rejectedId = repository.findAllPending(1L).stream()
                    .filter(prize -> prize.getTargetId() == 20L)
                    .findFirst()
                    .orElseThrow()
                    .getId();
            assertTrue(repository.reject(1L, rejectedId, 99L, 400L));

            assertEquals(java.util.Set.of(10L), repository.findPendingTargetIds(1L));
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = DSL.using(connection, SQLDialect.SQLITE);
        AccumulatorPrizesTable.ACCUMULATOR_PRIZES.getSchema(ctx).execute();
        return ctx;
    }

    private AccumulatorPrize money(long targetId, int amount, long createdAt) {
        return new AccumulatorPrize(1L, targetId, 5L, AccumulatorPrizeType.MONEY, amount, null, createdAt);
    }

    private AccumulatorPrize color(long targetId, long createdAt) {
        return new AccumulatorPrize(1L, targetId, 5L, AccumulatorPrizeType.COLOR_ROLE, null, 3600L, createdAt);
    }
}
