package ofc.bot.domain.database.repository;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.VoiceChannelIncomeRule;
import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;
import ofc.bot.domain.tables.VoiceChannelIncomeRulesTable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoiceChannelIncomeRuleRepositoryTest {
    @Test
    void shouldPersistAndFindRulesByPayoutType() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            VoiceChannelIncomeRuleRepository repository = new VoiceChannelIncomeRuleRepository(setup(connection));

            repository.save(rule(10L, VoiceChannelIncomePayoutType.MONEY, 1.25D));
            repository.save(rule(20L, VoiceChannelIncomePayoutType.LEVEL_EXPERIENCE, 1.50D));

            List<VoiceChannelIncomeRule> moneyRules = repository.findByPayoutType(VoiceChannelIncomePayoutType.MONEY);

            assertEquals(1, moneyRules.size());
            assertEquals(10L, moneyRules.getFirst().getChannelId());
            assertEquals(1.25D, moneyRules.getFirst().getMultiplier());
            assertTrue(moneyRules.getFirst().allowsMuted());
            assertTrue(moneyRules.getFirst().allowsSolo());
        }
    }

    @Test
    void shouldFindRuleByChannelAndType() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            VoiceChannelIncomeRuleRepository repository = new VoiceChannelIncomeRuleRepository(setup(connection));

            repository.save(rule(10L, VoiceChannelIncomePayoutType.MONEY, 1.25D));

            VoiceChannelIncomeRule rule = repository.findByChannelAndType(10L, VoiceChannelIncomePayoutType.MONEY);

            assertNotNull(rule);
            assertEquals(VoiceChannelIncomePayoutType.MONEY, rule.getPayoutType());
            assertEquals(5L, rule.getCreatedBy());
            assertEquals(100L, rule.getTimeCreated());
            assertEquals(100L, rule.getLastUpdated());
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        VoiceChannelIncomeRulesTable.VOICE_CHANNEL_INCOME_RULES.getSchema(ctx).execute();
        return ctx;
    }

    private VoiceChannelIncomeRule rule(
            long channelId,
            VoiceChannelIncomePayoutType payoutType,
            double multiplier
    ) {
        return new VoiceChannelIncomeRule(1L, channelId, payoutType, multiplier, true, true, 5L, 100L);
    }
}
