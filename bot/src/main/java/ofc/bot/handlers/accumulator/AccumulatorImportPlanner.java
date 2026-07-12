package ofc.bot.handlers.accumulator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongPredicate;

public final class AccumulatorImportPlanner {
    public List<Long> validTargetIds(String content) {
        return lines(content).stream()
                .map(String::strip)
                .map(this::parseId)
                .filter(Objects::nonNull)
                .toList();
    }

    public ImportPlan plan(
            String content,
            DuplicatePolicy duplicatePolicy,
            Set<Long> pendingTargetIds,
            LongPredicate memberExists
    ) {
        List<String> lines = lines(content);
        List<Long> acceptedIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String raw = lines.get(i).strip();
            Long userId = parseId(raw);

            if (userId == null) {
                errors.add("Linha " + lineNumber + " (`" + raw + "`): ID inválido.");
                continue;
            }

            if (duplicatePolicy == DuplicatePolicy.FORBID && !seen.add(userId)) {
                errors.add(userId + ": ID duplicado na importação.");
                continue;
            }

            if (duplicatePolicy == DuplicatePolicy.FORBID && pendingTargetIds.contains(userId)) {
                errors.add(userId + ": já possui prêmio pendente.");
                continue;
            }

            if (!memberExists.test(userId)) {
                errors.add(userId + ": membro não encontrado no servidor.");
                continue;
            }

            acceptedIds.add(userId);
        }

        return new ImportPlan(lines.size(), acceptedIds, errors);
    }

    private List<String> lines(String content) {
        return content == null ? List.of() : content.lines().toList();
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            long value = Long.parseLong(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public enum DuplicatePolicy {
        ALLOW,
        FORBID;

        public static DuplicatePolicy fromName(String name) {
            if (name == null) {
                return ALLOW;
            }

            for (DuplicatePolicy policy : values()) {
                if (policy.name().equalsIgnoreCase(name)) {
                    return policy;
                }
            }
            return ALLOW;
        }
    }

    public record ImportPlan(int totalIds, List<Long> acceptedIds, List<String> errors) {
        public int addedCount() {
            return acceptedIds.size();
        }
    }
}
