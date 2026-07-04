package ofc.bot.handlers.nick;

import ofc.bot.testing.MySQLTestDatabase;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import ofc.bot.domain.entity.MemberEmoji;
import ofc.bot.domain.entity.UserEmojiPermission;
import ofc.bot.domain.database.repository.MemberEmojiRepository;
import ofc.bot.domain.database.repository.UserEmojiPermissionRepository;
import ofc.bot.domain.tables.MembersEmojisTable;
import ofc.bot.domain.tables.UsersEmojisPermissionsTable;
import ofc.bot.util.content.Staff;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NicknameEmojiSanitizerTest {
    private static final String STAFF_EMOJI = "\uD83D\uDC51";
    private static final String OTHER_STAFF_EMOJI = "\uD83D\uDC8E";
    private static final String FREE_EMOJI = "\uD83D\uDE00";

    @Test
    void shouldRemoveUnauthorizedStaffEmojiFromNickname() throws Exception {
        try (TestContext test = createContext("Reserva")) {
            test.saveStaffEmoji(100L, STAFF_EMOJI);

            Optional<String> sanitized = test.sanitizer.sanitize(member(200L, false), "Ana " + STAFF_EMOJI, "Ana");

            assertEquals(Optional.of("Ana"), sanitized);
        }
    }

    @Test
    void shouldAllowUnownedEmoji() throws Exception {
        try (TestContext test = createContext("Reserva")) {
            Optional<String> sanitized = test.sanitizer.sanitize(member(200L, false), "Ana " + FREE_EMOJI, "Ana");

            assertTrue(sanitized.isEmpty());
        }
    }

    @Test
    void shouldAllowAuthorizedStaffEmoji() throws Exception {
        try (TestContext test = createContext("Reserva")) {
            test.saveStaffEmoji(100L, STAFF_EMOJI);
            test.permissionRepo.save(new UserEmojiPermission(100L, 200L, STAFF_EMOJI, 1_710_000_000_000L));

            Optional<String> sanitized = test.sanitizer.sanitize(member(200L, false), "Ana " + STAFF_EMOJI, "Ana");

            assertTrue(sanitized.isEmpty());
        }
    }

    @Test
    void shouldAllowStaffMemberToUseAnyStaffEmoji() throws Exception {
        try (TestContext test = createContext("Reserva")) {
            test.saveStaffEmoji(100L, STAFF_EMOJI);

            Optional<String> sanitized = test.sanitizer.sanitize(member(200L, true), "Ana " + STAFF_EMOJI, "Ana");

            assertTrue(sanitized.isEmpty());
        }
    }

    @Test
    void shouldUseFallbackNameWhenNicknameBecomesBlank() throws Exception {
        try (TestContext test = createContext("Reserva")) {
            test.saveStaffEmoji(100L, STAFF_EMOJI);

            Optional<String> sanitized = test.sanitizer.sanitize(member(200L, false), STAFF_EMOJI, "Ana");

            assertEquals(Optional.of("Ana"), sanitized);
        }
    }

    @Test
    void shouldUseConfiguredReplacementWhenNicknameAndFallbackBecomeBlank() throws Exception {
        try (TestContext test = createContext("Reserva")) {
            test.saveStaffEmoji(100L, STAFF_EMOJI);
            test.saveStaffEmoji(101L, OTHER_STAFF_EMOJI);

            Optional<String> sanitized = test.sanitizer.sanitize(member(200L, false), STAFF_EMOJI, OTHER_STAFF_EMOJI);

            assertEquals(Optional.of("Reserva"), sanitized);
        }
    }

    @Test
    void shouldIgnoreCleanFallbackWhenNoNicknameExists() throws Exception {
        try (TestContext test = createContext("Reserva")) {
            Optional<String> sanitized = test.sanitizer.sanitize(member(200L, false), null, "Ana");

            assertTrue(sanitized.isEmpty());
        }
    }

    private TestContext createContext(String... replacements) throws Exception {
        Connection connection = MySQLTestDatabase.open();
        DSLContext ctx = MySQLTestDatabase.context(connection);
        MembersEmojisTable.MEMBERS_EMOJIS.getSchema(ctx).execute();
        UsersEmojisPermissionsTable.USERS_EMOJIS_PERMS.getSchema(ctx).execute();

        MemberEmojiRepository emojiRepo = new MemberEmojiRepository(ctx);
        UserEmojiPermissionRepository permissionRepo = new UserEmojiPermissionRepository(ctx);
        NicknameEmojiPolicy policy = new NicknameEmojiPolicy(emojiRepo, permissionRepo);
        NicknameEmojiSanitizer sanitizer = new NicknameEmojiSanitizer(policy, () -> replacements);

        return new TestContext(connection, emojiRepo, permissionRepo, sanitizer);
    }

    private static Member member(long userId, boolean staff) {
        List<Role> roles = staff ? List.of(role(Staff.GENERAL.getId())) : List.of();
        return (Member) Proxy.newProxyInstance(
                Member.class.getClassLoader(),
                new Class<?>[] { Member.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getIdLong" -> userId;
                    case "getRoles" -> roles;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static Role role(String roleId) {
        return (Role) Proxy.newProxyInstance(
                Role.class.getClassLoader(),
                new Class<?>[] { Role.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> roleId;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private record TestContext(
            Connection connection,
            MemberEmojiRepository emojiRepo,
            UserEmojiPermissionRepository permissionRepo,
            NicknameEmojiSanitizer sanitizer
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
