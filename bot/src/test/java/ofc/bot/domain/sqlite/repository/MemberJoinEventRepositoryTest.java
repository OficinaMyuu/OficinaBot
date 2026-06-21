package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.entity.MemberJoinEvent;
import ofc.bot.domain.tables.MemberJoinEventsTable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class MemberJoinEventRepositoryTest {
    @Test
    void shouldPersistMultipleJoinEventsForSameUser() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            MemberJoinEventRepository repository = new MemberJoinEventRepository(setup(connection));

            repository.save(gatewayEvent(10L, 200L));
            repository.save(gatewayEvent(10L, 300L));

            assertEquals(2, repository.countAll());
            assertEquals(200L, repository.findEarliestCreatedAtByGuildAndUserId(1L, 10L));
        }
    }

    @Test
    void shouldSeparateEarliestJoinByGuild() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            MemberJoinEventRepository repository = new MemberJoinEventRepository(setup(connection));

            repository.save(new MemberJoinEvent(1L, 10L, 300L));
            repository.save(new MemberJoinEvent(2L, 10L, 200L));

            assertEquals(300L, repository.findEarliestCreatedAtByGuildAndUserId(1L, 10L));
            assertEquals(200L, repository.findEarliestCreatedAtByGuildAndUserId(2L, 10L));
        }
    }

    @Test
    void shouldReturnNullWhenNoJoinHistoryExists() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            MemberJoinEventRepository repository = new MemberJoinEventRepository(setup(connection));

            assertNull(repository.findEarliestCreatedAtByGuildAndUserId(1L, 10L));
        }
    }

    private DSLContext setup(Connection connection) {
        DSLContext ctx = DSL.using(connection, SQLDialect.SQLITE);
        MemberJoinEventsTable.MEMBER_JOIN_EVENTS.getSchema(ctx).execute();
        return ctx;
    }

    private MemberJoinEvent gatewayEvent(long userId, long createdAt) {
        return new MemberJoinEvent(
                1L,
                userId,
                createdAt
        );
    }
}
