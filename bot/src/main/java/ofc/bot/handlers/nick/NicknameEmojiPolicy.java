package ofc.bot.handlers.nick;

import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.entities.Member;
import ofc.bot.domain.entity.MemberEmoji;
import ofc.bot.domain.entity.UserEmojiPermission;
import ofc.bot.domain.database.repository.MemberEmojiRepository;
import ofc.bot.domain.database.repository.UserEmojiPermissionRepository;
import ofc.bot.util.content.Staff;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class NicknameEmojiPolicy {
    public static final int MAX_EMOJIS = 3;

    private final MemberEmojiRepository memberEmojiRepo;
    private final UserEmojiPermissionRepository emojiPermissionRepo;

    public NicknameEmojiPolicy(
            MemberEmojiRepository memberEmojiRepo,
            UserEmojiPermissionRepository emojiPermissionRepo
    ) {
        this.memberEmojiRepo = Objects.requireNonNull(memberEmojiRepo);
        this.emojiPermissionRepo = Objects.requireNonNull(emojiPermissionRepo);
    }

    public NicknameEmojiReport inspect(@NotNull Member target, @NotNull String nickname) {
        NicknameEmojiReport report = inspect(target.getIdLong(), nickname);
        if (!Staff.isStaff(target)) {
            return report;
        }

        return new NicknameEmojiReport(nickname, report.emojis(), List.of(), List.of());
    }

    public NicknameEmojiReport inspect(long targetUserId, @NotNull String nickname) {
        List<String> emojis = EmojiParser.extractEmojis(nickname);
        if (emojis.isEmpty()) {
            return new NicknameEmojiReport(nickname, emojis, List.of(), List.of());
        }

        Set<String> uniqueEmojis = new LinkedHashSet<>(emojis);
        Map<String, Long> staffEmojiOwners = memberEmojiRepo.findByEmojis(uniqueEmojis)
                .stream()
                .collect(Collectors.toMap(
                        MemberEmoji::getEmoji,
                        MemberEmoji::getUserId,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        List<ApprovedEmojiUse> approved = new ArrayList<>();
        List<UnauthorizedEmojiUse> unauthorized = new ArrayList<>();
        Set<String> relevantStaffEmojis = new LinkedHashSet<>();

        for (String emoji : emojis) {
            Long ownerId = staffEmojiOwners.get(emoji);
            if (ownerId == null || ownerId == targetUserId) {
                continue;
            }
            relevantStaffEmojis.add(emoji);
        }

        Map<String, UserEmojiPermission> permissions = emojiPermissionRepo.findByUserAndEmojis(
                targetUserId,
                relevantStaffEmojis
        );

        for (String emoji : relevantStaffEmojis) {
            Long ownerId = staffEmojiOwners.get(emoji);
            if (ownerId == null || ownerId == targetUserId) {
                continue;
            }

            UserEmojiPermission permission = permissions.get(emoji);
            if (permission == null) {
                unauthorized.add(new UnauthorizedEmojiUse(emoji, ownerId));
                continue;
            }

            approved.add(new ApprovedEmojiUse(emoji, ownerId, permission.getTimeCreated()));
        }

        return new NicknameEmojiReport(nickname, emojis, approved, unauthorized);
    }

    public record NicknameEmojiReport(
            String nickname,
            List<String> emojis,
            List<ApprovedEmojiUse> approvedStaffEmojis,
            List<UnauthorizedEmojiUse> unauthorizedStaffEmojis
    ) {
        public int emojiCount() {
            return emojis.size();
        }

        public boolean hasEmojis() {
            return emojiCount() > 0;
        }

        public boolean hasTooManyEmojis() {
            return emojiCount() > MAX_EMOJIS;
        }

        public boolean hasUnauthorizedStaffEmojis() {
            return !unauthorizedStaffEmojis.isEmpty();
        }

        public boolean hasApprovedStaffEmojis() {
            return !approvedStaffEmojis.isEmpty();
        }

        public boolean isAccepted() {
            return !hasTooManyEmojis() && !hasUnauthorizedStaffEmojis();
        }

        public String approvedSummary() {
            if (approvedStaffEmojis.isEmpty()) {
                return null;
            }

            return approvedStaffEmojis.stream()
                    .map(emoji -> String.format("%s <t:%d:f>", emoji.emoji(), toDiscordSeconds(emoji.approvedAt())))
                    .collect(Collectors.joining("\n"));
        }

        public String unauthorizedSummary() {
            if (unauthorizedStaffEmojis.isEmpty()) {
                return null;
            }

            return unauthorizedStaffEmojis.stream()
                    .map(emoji -> String.format("%s de <@%d>", emoji.emoji(), emoji.ownerId()))
                    .collect(Collectors.joining("\n"));
        }

        private static long toDiscordSeconds(long timestamp) {
            return timestamp > 100_000_000_000L ? timestamp / 1000 : timestamp;
        }
    }

    public record ApprovedEmojiUse(String emoji, long ownerId, long approvedAt) {}

    public record UnauthorizedEmojiUse(String emoji, long ownerId) {}
}
