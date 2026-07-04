package ofc.bot.domain.database.repository;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.domain.entity.enums.GiveawayStatus;
import ofc.bot.domain.entity.enums.GiveawayWinnerStatus;
import ofc.bot.domain.tables.GiveawayEntriesTable;
import ofc.bot.domain.tables.GiveawayWinnersTable;
import ofc.bot.domain.tables.GiveawaysTable;
import ofc.bot.handlers.economy.CurrencyType;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GiveawayRepositoryTest {
    @Test
    void shouldPersistGiveawayEntriesAndEndState() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            GiveawayRepository giveaways = new GiveawayRepository(ctx);
            GiveawayEntryRepository entries = new GiveawayEntryRepository(ctx);

            giveaways.save(giveaway("g-1", GiveawayStatus.ACTIVE, 1000L));

            assertTrue(entries.addEntry("g-1", 10L, 100L));
            assertFalse(entries.addEntry("g-1", 10L, 101L));
            assertTrue(entries.addEntry("g-1", 20L, 102L));
            assertEquals(2, entries.countByGiveaway("g-1"));
            assertEquals(1, giveaways.findDueActive(1000L).size());

            assertTrue(giveaways.markEnded("g-1", 1100L));
            assertFalse(giveaways.markEnded("g-1", 1101L));
            assertEquals(GiveawayStatus.ENDED, giveaways.findById("g-1").getStatus());
        }
    }

    @Test
    void shouldRemoveVoiceLockedEntriesOnlyForMatchingActiveGiveaways() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            GiveawayRepository giveaways = new GiveawayRepository(ctx);
            GiveawayEntryRepository entries = new GiveawayEntryRepository(ctx);

            Giveaway voiceLocked = giveaway("g-2", GiveawayStatus.ACTIVE, 2000L)
                    .setRequiredVoiceChannelId(50L);
            Giveaway otherVoice = giveaway("g-3", GiveawayStatus.ACTIVE, 2000L)
                    .setRequiredVoiceChannelId(51L);

            giveaways.save(voiceLocked);
            giveaways.save(otherVoice);
            entries.addEntry("g-2", 10L, 100L);
            entries.addEntry("g-3", 10L, 100L);

            assertEquals(List.of("g-2"), entries.findActiveGiveawaysForVoiceChannel(50L));
            assertEquals(1, entries.removeUserFromVoiceLockedGiveaways(10L, 50L));
            assertFalse(entries.hasEntry("g-2", 10L));
            assertTrue(entries.hasEntry("g-3", 10L));
        }
    }

    @Test
    void shouldMoveWinnerThroughClaimStates() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = setup(connection);
            GiveawayWinnerRepository winners = new GiveawayWinnerRepository(ctx);

            winners.saveWinners("g-4", List.of(10L), GiveawayWinnerStatus.PENDING_CLAIM, 100L);

            assertTrue(winners.startClaim("g-4", 10L, 110L));
            assertFalse(winners.startClaim("g-4", 10L, 111L));
            assertTrue(winners.markClaimed("g-4", 10L, CurrencyType.OFICINA, null, 120L));

            var winner = winners.findActiveWinner("g-4", 10L);
            assertEquals(GiveawayWinnerStatus.CLAIMED, winner.getStatus());
            assertEquals(CurrencyType.OFICINA, winner.getCurrency());
            assertEquals(120L, winner.getClaimedAt());
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = MySQLTestDatabase.context(connection);
        GiveawaysTable.GIVEAWAYS.getSchema(ctx).execute();
        GiveawayEntriesTable.GIVEAWAY_ENTRIES.getSchema(ctx).execute();
        GiveawayWinnersTable.GIVEAWAY_WINNERS.getSchema(ctx).execute();
        return ctx;
    }

    private Giveaway giveaway(String id, GiveawayStatus status, long endsAt) {
        return new Giveaway(
                id,
                1L,
                Math.abs(id.hashCode()) + 10L,
                Math.abs(id.hashCode()) + 20L,
                30L,
                status,
                GiveawayPrizeType.GENERIC,
                "Giveaway",
                "Prize",
                1,
                endsAt,
                status == GiveawayStatus.ACTIVE ? null : endsAt,
                null,
                null,
                null,
                100L,
                100L
        );
    }
}
