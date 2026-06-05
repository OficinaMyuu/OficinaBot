package ofc.bot.handlers.nick;

import net.dv8tion.jda.api.entities.Member;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class NicknameEmojiSanitizer {
    private static final int MAX_NICKNAME_LENGTH = 32;
    private static final String REPLACEMENTS_KEY = "nicks.replacements";

    private final NicknameEmojiPolicy emojiPolicy;
    private final Supplier<String[]> replacementSupplier;

    public NicknameEmojiSanitizer(@NotNull NicknameEmojiPolicy emojiPolicy) {
        this(emojiPolicy, () -> Bot.getArray(REPLACEMENTS_KEY));
    }

    NicknameEmojiSanitizer(
            @NotNull NicknameEmojiPolicy emojiPolicy,
            @NotNull Supplier<String[]> replacementSupplier
    ) {
        this.emojiPolicy = Objects.requireNonNull(emojiPolicy);
        this.replacementSupplier = Objects.requireNonNull(replacementSupplier);
    }

    public Optional<String> sanitize(
            @NotNull Member member,
            @Nullable String candidateName,
            @Nullable String fallbackName
    ) {
        boolean candidateWasBlank = isBlank(normalize(candidateName));
        String sanitized = sanitizeName(member, candidateName);
        if (!isBlank(sanitized)) {
            return changed(candidateName, sanitized);
        }

        String sanitizedFallback = sanitizeName(member, fallbackName);
        if (!isBlank(sanitizedFallback)) {
            return candidateWasBlank ? changed(fallbackName, sanitizedFallback) : Optional.of(sanitizedFallback);
        }

        if (candidateWasBlank && isBlank(normalize(fallbackName))) {
            return Optional.empty();
        }

        String replacement = randomReplacement(member);
        return isBlank(replacement) ? Optional.empty() : Optional.of(replacement);
    }

    private Optional<String> changed(String original, String sanitized) {
        if (Objects.equals(normalize(original), sanitized)) {
            return Optional.empty();
        }

        return Optional.of(sanitized);
    }

    private String randomReplacement(Member member) {
        String[] replacements = replacementSupplier.get();
        if (replacements == null || replacements.length == 0) {
            return null;
        }

        int offset = ThreadLocalRandom.current().nextInt(replacements.length);
        for (int i = 0; i < replacements.length; i++) {
            String replacement = replacements[(offset + i) % replacements.length];
            String sanitized = sanitizeName(member, replacement);
            if (!isBlank(sanitized)) {
                return sanitized;
            }
        }

        return null;
    }

    private String sanitizeName(Member member, String name) {
        String sanitized = normalize(name);
        if (sanitized == null) {
            return null;
        }

        NicknameEmojiPolicy.NicknameEmojiReport report = emojiPolicy.inspect(member, sanitized);
        for (NicknameEmojiPolicy.UnauthorizedEmojiUse use : report.unauthorizedStaffEmojis()) {
            sanitized = sanitized.replace(use.emoji(), "");
        }

        return limit(normalize(sanitized));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.replaceAll("\\s+", " ").strip();
        return normalized.isBlank() ? null : normalized;
    }

    private static String limit(String value) {
        if (value == null || value.codePointCount(0, value.length()) <= MAX_NICKNAME_LENGTH) {
            return value;
        }

        int end = value.offsetByCodePoints(0, MAX_NICKNAME_LENGTH);
        return normalize(value.substring(0, end));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
