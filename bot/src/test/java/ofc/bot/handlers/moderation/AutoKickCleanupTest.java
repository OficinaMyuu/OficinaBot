package ofc.bot.handlers.moderation;

import ofc.bot.domain.entity.UserXP;
import ofc.bot.domain.sqlite.repository.UserXPRepository;
import ofc.bot.domain.tables.UsersTable;
import ofc.bot.domain.tables.UsersXPTable;
import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.BankAction;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.economy.PaymentManager;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoKickCleanupTest {
    private static final UsersTable USERS = UsersTable.USERS;
    private static final UsersXPTable USERS_XP = UsersXPTable.USERS_XP;
    private Connection connection;

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void shouldDeleteUserXpAndResetEveryEconomy() throws SQLException {
        long userId = 42L;
        long guildId = 100L;
        String reason = "4 warnings";
        DSLContext ctx = createContext();
        UserXPRepository xpRepo = new UserXPRepository(ctx);
        RecordingPaymentManager oficina = new RecordingPaymentManager(CurrencyType.OFICINA);
        RecordingPaymentManager unbelievaBoat = new RecordingPaymentManager(CurrencyType.UNBELIEVABOAT);

        createUser(ctx, userId);
        xpRepo.upsert(new UserXP(37, 5, userId, 1, 2));

        AutoKickCleanup cleanup = new AutoKickCleanup(xpRepo, List.of(oficina, unbelievaBoat));
        cleanup.reset(userId, guildId, reason);

        assertNull(xpRepo.findByUserId(userId));
        assertEquals(List.of(new ResetCall(userId, guildId, 0, 0, reason)), oficina.calls);
        assertEquals(List.of(new ResetCall(userId, guildId, 0, 0, reason)), unbelievaBoat.calls);
    }

    @Test
    void shouldResetEconomiesEvenWhenUserHasNoXpRow() throws SQLException {
        long userId = 43L;
        long guildId = 100L;
        DSLContext ctx = createContext();
        UserXPRepository xpRepo = new UserXPRepository(ctx);
        RecordingPaymentManager bank = new RecordingPaymentManager(CurrencyType.OFICINA);

        AutoKickCleanup cleanup = new AutoKickCleanup(xpRepo, List.of(bank));
        cleanup.reset(userId, guildId, "4 warnings");

        assertNull(xpRepo.findByUserId(userId));
        assertEquals(1, bank.calls.size());
    }

    private DSLContext createContext() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        DSLContext ctx = DSL.using(connection, SQLDialect.SQLITE);
        USERS.getSchema(ctx).execute();
        USERS_XP.getSchema(ctx).execute();
        return ctx;
    }

    private void createUser(DSLContext ctx, long userId) {
        ctx.insertInto(USERS)
                .columns(USERS.ID, USERS.NAME, USERS.CREATED_AT, USERS.UPDATED_AT)
                .values(userId, "Test User", 1L, 1L)
                .execute();
    }

    private record ResetCall(long userId, long guildId, long cash, long bank, String reason) {}

    private static final class RecordingPaymentManager implements PaymentManager {
        private final CurrencyType currencyType;
        private final List<ResetCall> calls = new ArrayList<>();

        private RecordingPaymentManager(CurrencyType currencyType) {
            this.currencyType = currencyType;
        }

        @Override
        public BankAccount get(long userId, long guildId) {
            return null;
        }

        @Override
        public BankAccount set(long userId, long guildId, long cash, long bank, String reason) {
            calls.add(new ResetCall(userId, guildId, cash, bank, reason));
            return null;
        }

        @Override
        public BankAccount update(long userId, long guildId, long cash, long bank, String reason) {
            return null;
        }

        @Override
        public CurrencyType getCurrencyType() {
            return currencyType;
        }

        @Override
        public BankAction charge(long userId, long guildId, long cash, long bank, String reason) {
            return BankAction.STATIC_SUCCESS_NO_CHANGE;
        }
    }
}
