package ofc.bot.domain.database.repository;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.NicknameUpdateRequest;
import ofc.bot.domain.entity.enums.NicknameRequestStatus;
import ofc.bot.domain.tables.NicknameUpdateRequestsTable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class NicknameUpdateRequestRepositoryTest {
    @Test
    void shouldFindRequestByEitherButtonId() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = MySQLTestDatabase.context(connection);
            NicknameUpdateRequestsTable.NICKNAME_UPDATE_REQUESTS.getSchema(ctx).execute();
            NicknameUpdateRequestRepository repository = new NicknameUpdateRequestRepository(ctx);
            NicknameUpdateRequest request = request("req-1", "nick-req-1-approve", "nick-req-1-reject");

            repository.save(request);

            assertEquals("req-1", repository.findByButtonId("nick-req-1-approve").getRequestId());
            assertEquals("req-1", repository.findByButtonId("nick-req-1-reject").getRequestId());
        }
    }

    @Test
    void shouldMovePendingRequestThroughApprovalStates() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = MySQLTestDatabase.context(connection);
            NicknameUpdateRequestsTable.NICKNAME_UPDATE_REQUESTS.getSchema(ctx).execute();
            NicknameUpdateRequestRepository repository = new NicknameUpdateRequestRepository(ctx);
            repository.save(request("req-2", "nick-req-2-approve", "nick-req-2-reject"));

            assertTrue(repository.startProcessing("req-2", 30L, 2000L));
            assertFalse(repository.startProcessing("req-2", 31L, 2001L));
            assertTrue(repository.markApproved("req-2", 30L, 3000L));

            NicknameUpdateRequest saved = repository.findByRequestId("req-2");
            assertEquals(NicknameRequestStatus.APPROVED, saved.getStatus());
            assertEquals(30L, saved.getDecisionAuthorId());
            assertEquals(3000L, saved.getDecidedAt());
        }
    }

    @Test
    void shouldRejectOnlyPendingRequests() throws Exception {
        try (Connection connection = MySQLTestDatabase.open()) {
            DSLContext ctx = MySQLTestDatabase.context(connection);
            NicknameUpdateRequestsTable.NICKNAME_UPDATE_REQUESTS.getSchema(ctx).execute();
            NicknameUpdateRequestRepository repository = new NicknameUpdateRequestRepository(ctx);
            repository.save(request("req-3", "nick-req-3-approve", "nick-req-3-reject"));

            assertTrue(repository.markRejected("req-3", 40L, 4000L));
            assertFalse(repository.markRejected("req-3", 41L, 4001L));

            NicknameUpdateRequest saved = repository.findByRequestId("req-3");
            assertEquals(NicknameRequestStatus.REJECTED, saved.getStatus());
            assertEquals(40L, saved.getDecisionAuthorId());
        }
    }

    private NicknameUpdateRequest request(String id, String approveButtonId, String rejectButtonId) {
        return new NicknameUpdateRequest(
                id,
                1L,
                2L,
                Long.parseLong(id.substring(id.length() - 1)) + 10L,
                20L,
                30L,
                "Novo Nick",
                approveButtonId,
                rejectButtonId,
                NicknameRequestStatus.PENDING,
                null,
                null,
                1000L,
                1000L
        );
    }
}
