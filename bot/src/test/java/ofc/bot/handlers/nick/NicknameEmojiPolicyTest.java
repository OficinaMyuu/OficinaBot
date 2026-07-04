package ofc.bot.handlers.nick;

import ofc.bot.testing.MySQLTestDatabase;

import ofc.bot.domain.entity.MemberEmoji;
import ofc.bot.domain.entity.UserEmojiPermission;
import ofc.bot.domain.database.repository.MemberEmojiRepository;
import ofc.bot.domain.database.repository.UserEmojiPermissionRepository;
import ofc.bot.domain.tables.MembersEmojisTable;
import ofc.bot.domain.tables.UsersEmojisPermissionsTable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class NicknameEmojiPolicyTest {
    private static final String STAFF_EMOJI = "👑";

    @Test
    void shouldRejectNicknamesWithMoreThanThreeEmojis() throws Exception {
        try (TestContext test = createContext()) {
            NicknameEmojiPolicy.NicknameEmojiReport report = test.policy.inspect(10L, "Nome 😀😃😄😁");

            assertEquals(4, report.emojiCount());
            assertTrue(report.hasTooManyEmojis());
            assertFalse(report.isAccepted());
        }
    }

    @Test
    void shouldFindUnauthorizedStaffOwnedEmoji() throws Exception {
        try (TestContext test = createContext()) {
            test.saveStaffEmoji(100L, STAFF_EMOJI);

            NicknameEmojiPolicy.NicknameEmojiReport report = test.policy.inspect(200L, "Nome " + STAFF_EMOJI);

            assertTrue(report.hasUnauthorizedStaffEmojis());
            assertEquals(1, report.unauthorizedStaffEmojis().size());
            assertEquals(100L, report.unauthorizedStaffEmojis().getFirst().ownerId());
            assertFalse(report.isAccepted());
        }
    }

    @Test
    void shouldAcceptAuthorizedStaffOwnedEmoji() throws Exception {
        try (TestContext test = createContext()) {
            long approvedAtMillis = 1_710_000_000_000L;
            test.saveStaffEmoji(100L, STAFF_EMOJI);
            test.permissionRepo.save(new UserEmojiPermission(100L, 200L, STAFF_EMOJI, approvedAtMillis));

            NicknameEmojiPolicy.NicknameEmojiReport report = test.policy.inspect(200L, "Nome " + STAFF_EMOJI);

            assertTrue(report.isAccepted());
            assertTrue(report.hasApprovedStaffEmojis());
            assertEquals(STAFF_EMOJI + " <t:1710000000:f>", report.approvedSummary());
        }
    }

    @Test
    void shouldAllowOwnerToUseOwnEmojiWithoutAuthorization() throws Exception {
        try (TestContext test = createContext()) {
            test.saveStaffEmoji(100L, STAFF_EMOJI);

            NicknameEmojiPolicy.NicknameEmojiReport report = test.policy.inspect(100L, "Nome " + STAFF_EMOJI);

            assertTrue(report.isAccepted());
            assertFalse(report.hasUnauthorizedStaffEmojis());
            assertFalse(report.hasApprovedStaffEmojis());
        }
    }

    private TestContext createContext() throws Exception {
        Connection connection = MySQLTestDatabase.open();
        DSLContext ctx = MySQLTestDatabase.context(connection);
        MembersEmojisTable.MEMBERS_EMOJIS.getSchema(ctx).execute();
        UsersEmojisPermissionsTable.USERS_EMOJIS_PERMS.getSchema(ctx).execute();

        MemberEmojiRepository emojiRepo = new MemberEmojiRepository(ctx);
        UserEmojiPermissionRepository permissionRepo = new UserEmojiPermissionRepository(ctx);
        NicknameEmojiPolicy policy = new NicknameEmojiPolicy(emojiRepo, permissionRepo);

        return new TestContext(connection, emojiRepo, permissionRepo, policy);
    }

    private record TestContext(
            Connection connection,
            MemberEmojiRepository emojiRepo,
            UserEmojiPermissionRepository permissionRepo,
            NicknameEmojiPolicy policy
    ) implements AutoCloseable {
        void saveStaffEmoji(long ownerId, String emoji) {
            long now = 1_710_000_000_000L;
            MemberEmoji record = new MemberEmoji()
                    .setUserId(ownerId)
                    .setEmoji(emoji)
                    .setLastUpdated(now);

            record.set(MembersEmojisTable.MEMBERS_EMOJIS.CREATED_AT, now);
            emojiRepo.save(record);
        }

        @Override
        public void close() throws Exception {
            connection.close();
        }
    }
}
