package ofc.bot.handlers.accumulator;

import ofc.bot.testing.MySQLTestDatabase;

import net.dv8tion.jda.api.entities.Guild;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.enums.AccumulatorPrizeStatus;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.database.repository.AccumulatorPrizeRepository;
import ofc.bot.domain.database.repository.ColorRoleItemRepository;
import ofc.bot.domain.database.repository.ColorRoleStateRepository;
import ofc.bot.domain.tables.AccumulatorPrizesTable;
import ofc.bot.domain.tables.ColorRoleItemsTable;
import ofc.bot.domain.tables.ColorRolesStateTable;
import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.BankAction;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.economy.PaymentManager;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccumulatorPayoutServiceTest {
    @Test
    void shouldBlockApproveAllWhenAnyPrizeIsNotConfigured() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            TestRepos repos = setup(connection);
            FakePayment payment = new FakePayment();
            FakeBridge bridge = new FakeBridge().member(10L).member(20L);
            AccumulatorPayoutService service = service(repos, payment, bridge);

            repos.prizes.save(money(10L, 500, CurrencyType.OFICINA));
            repos.prizes.save(color(20L, null));

            AccumulatorApprovalReport report = service.approveAll(1L, null, 99L);

            assertFalse(report.successful());
            assertEquals(0, report.paid());
            assertEquals(2, repos.prizes.countPending(1L));
            assertEquals(0, payment.bank(10L));
            assertTrue(report.details().getFirst().contains("escolha um cargo de cor"));
        }
    }

    @Test
    void shouldPayMoneyAndColorRowsThenMarkApprovedBySameUser() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            TestRepos repos = setup(connection);
            FakePayment payment = new FakePayment();
            FakeBridge bridge = new FakeBridge().member(10L).member(20L).role(500L);
            AccumulatorPayoutService service = service(repos, payment, bridge);

            repos.colors.save(colorItem(500L));
            repos.prizes.save(money(10L, 700, CurrencyType.UNBELIEVABOAT));
            repos.prizes.save(color(20L, 500L));

            AccumulatorApprovalReport report = service.approveAll(1L, null, 99L);

            assertTrue(report.successful());
            assertEquals(2, report.paid());
            assertEquals(700, payment.bank(10L));
            assertTrue(bridge.memberHasRole(null, 20L, 500L));
            assertEquals(1, repos.states.findByUserId(20L).size());
            assertEquals(0, repos.prizes.countPending(1L));

            for (AccumulatorPrize prize : repos.prizes.findAll()) {
                assertEquals(AccumulatorPrizeStatus.PAID, prize.getStatus());
                assertEquals(99L, prize.getApprovedBy());
            }
        }
    }

    @Test
    void shouldRollbackExternalChangesWhenLaterPrizeFails() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            TestRepos repos = setup(connection);
            FakePayment payment = new FakePayment().failUser(20L);
            FakeBridge bridge = new FakeBridge().member(10L).member(20L);
            AccumulatorPayoutService service = service(repos, payment, bridge);

            repos.prizes.save(money(10L, 900, CurrencyType.OFICINA));
            repos.prizes.save(money(20L, 100, CurrencyType.OFICINA));

            AccumulatorApprovalReport report = service.approveAll(1L, null, 99L);

            assertFalse(report.successful());
            assertEquals(0, report.paid());
            assertEquals(0, payment.bank(10L));
            assertEquals(2, repos.prizes.countPending(1L));
        }
    }

    private AccumulatorPayoutService service(TestRepos repos, FakePayment payment, FakeBridge bridge) {
        return new AccumulatorPayoutService(
                repos.prizes,
                repos.colors,
                repos.states,
                ignored -> payment,
                bridge
        );
    }

    private TestRepos setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        AccumulatorPrizesTable.ACCUMULATOR_PRIZES.getSchema(ctx).execute();
        ColorRoleItemsTable.COLOR_ROLE_ITEMS.getSchema(ctx).execute();
        ColorRolesStateTable.COLOR_ROLES_STATES.getSchema(ctx).execute();
        return new TestRepos(
                new AccumulatorPrizeRepository(ctx),
                new ColorRoleItemRepository(ctx),
                new ColorRoleStateRepository(ctx)
        );
    }

    private AccumulatorPrize money(long targetId, int amount, CurrencyType currency) {
        AccumulatorPrize prize = new AccumulatorPrize(
                1L,
                targetId,
                5L,
                AccumulatorPrizeType.MONEY,
                amount,
                null,
                targetId
        );
        prize.setCurrency(currency);
        return prize;
    }

    private AccumulatorPrize color(long targetId, Long roleId) {
        AccumulatorPrize prize = new AccumulatorPrize(
                1L,
                targetId,
                5L,
                AccumulatorPrizeType.COLOR_ROLE,
                null,
                3600L,
                targetId
        );
        prize.setColorRoleId(roleId);
        return prize;
    }

    private ColorRoleItem colorItem(long roleId) {
        ColorRoleItem item = new ColorRoleItem();
        ColorRoleItemsTable table = ColorRoleItemsTable.COLOR_ROLE_ITEMS;
        item.set(table.PRICE, 0);
        item.set(table.ROLE_ID, roleId);
        item.set(table.CREATED_AT, 100L);
        item.set(table.UPDATED_AT, 100L);
        return item;
    }

    private record TestRepos(
            AccumulatorPrizeRepository prizes,
            ColorRoleItemRepository colors,
            ColorRoleStateRepository states
    ) {}

    private static class FakeBridge implements AccumulatorDiscordBridge {
        private final Set<Long> members = new HashSet<>();
        private final Set<Long> roles = new HashSet<>();
        private final Set<String> memberRoles = new HashSet<>();

        FakeBridge member(long userId) {
            members.add(userId);
            return this;
        }

        FakeBridge role(long roleId) {
            roles.add(roleId);
            return this;
        }

        @Override
        public boolean memberExists(Guild guild, long userId) {
            return members.contains(userId);
        }

        @Override
        public boolean roleExists(Guild guild, long roleId) {
            return roles.contains(roleId);
        }

        @Override
        public boolean memberHasRole(Guild guild, long userId, long roleId) {
            return memberRoles.contains(key(userId, roleId));
        }

        @Override
        public void addRole(Guild guild, long userId, long roleId) {
            if (!memberExists(guild, userId) || !roleExists(guild, roleId)) {
                throw new IllegalStateException("member or role missing");
            }
            memberRoles.add(key(userId, roleId));
        }

        @Override
        public void removeRole(Guild guild, long userId, long roleId) {
            memberRoles.remove(key(userId, roleId));
        }

        private String key(long userId, long roleId) {
            return userId + ":" + roleId;
        }
    }

    private static class FakePayment implements PaymentManager {
        private final Map<Long, Long> banks = new HashMap<>();
        private final Set<Long> failUsers = new HashSet<>();

        FakePayment failUser(long userId) {
            failUsers.add(userId);
            return this;
        }

        long bank(long userId) {
            return banks.getOrDefault(userId, 0L);
        }

        @Override
        public BankAccount get(long userId, long guildId) {
            return account(userId, guildId);
        }

        @Override
        public BankAccount set(long userId, long guildId, long cash, long bank, String reason) {
            banks.put(userId, bank);
            return account(userId, guildId);
        }

        @Override
        public BankAccount update(long userId, long guildId, long cash, long bank, String reason) {
            if (failUsers.contains(userId)) {
                throw new IllegalStateException("economy unavailable");
            }
            banks.merge(userId, bank, Long::sum);
            return account(userId, guildId);
        }

        @Override
        public CurrencyType getCurrencyType() {
            return CurrencyType.OFICINA;
        }

        @Override
        public BankAction charge(long userId, long guildId, long cash, long bank, String reason) {
            return BankAction.STATIC_SUCCESS_NO_CHANGE;
        }

        private BankAccount account(long userId, long guildId) {
            long bank = bank(userId);
            return new BankAccount() {
                @Override
                public long getUserId() { return userId; }

                @Override
                public long getGuildId() { return guildId; }

                @Override
                public long getCash() { return 0; }

                @Override
                public long getBank() { return bank; }

                @Override
                public long getTotal() { return bank; }

                @Override
                public int getRank() { return 1; }

                @Override
                public CurrencyType getType() { return CurrencyType.OFICINA; }

                @Override
                public BankAccount setCash(long cash) { return this; }

                @Override
                public BankAccount modifyCash(long cash) { return this; }

                @Override
                public BankAccount setBank(long bank) { return this; }

                @Override
                public BankAccount modifyBank(long bank) { return this; }

                @Override
                public boolean isDummy() { return false; }
            };
        }
    }
}
