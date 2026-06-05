package ofc.bot.handlers.nick;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class NicknameEmojiEnforcer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NicknameEmojiEnforcer.class);

    private final NicknameEmojiSanitizer sanitizer;

    public NicknameEmojiEnforcer(@NotNull NicknameEmojiSanitizer sanitizer) {
        this.sanitizer = Objects.requireNonNull(sanitizer);
    }

    public void enforce(
            @NotNull Member member,
            @Nullable String candidateName,
            @Nullable String fallbackName,
            @NotNull String reason
    ) {
        if (member.getUser().isBot()) {
            return;
        }

        Member self = member.getGuild().getSelfMember();
        if (!self.hasPermission(Permission.NICKNAME_MANAGE) || !self.canInteract(member)) {
            return;
        }

        Optional<String> sanitized = sanitizer.sanitize(member, candidateName, fallbackName);
        if (sanitized.isEmpty() || Objects.equals(member.getNickname(), sanitized.get())) {
            return;
        }

        member.modifyNickname(sanitized.get())
                .reason(reason)
                .queue(null, error -> LOGGER.warn(
                        "Failed to enforce nickname emoji policy for member {}",
                        member.getIdLong(),
                        error
                ));
    }
}
