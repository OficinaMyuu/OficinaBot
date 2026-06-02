package ofc.bot.handlers.tickets;

import java.util.List;

public final class TicketMemberUpdatePolicy {
    private TicketMemberUpdatePolicy() {}

    public static Decision decide(TicketMemberAction action, Candidate candidate, long initiatorId) {
        return switch (action) {
            case ADD -> candidate.hasTicketAccess()
                    ? Decision.skip(SkipReason.ALREADY_IN_TICKET)
                    : Decision.apply();
            case REMOVE -> decideRemoval(candidate, initiatorId);
        };
    }

    public static Summary summarize(TicketMemberAction action, List<Decision> decisions) {
        int applied = 0;
        int skipped = 0;

        for (Decision decision : decisions) {
            if (decision.shouldApply()) {
                applied++;
            } else {
                skipped++;
            }
        }

        return new Summary(action, applied, skipped);
    }

    private static Decision decideRemoval(Candidate candidate, long initiatorId) {
        if (candidate.userId() == initiatorId) {
            return Decision.skip(SkipReason.TICKET_INITIATOR);
        }

        if (candidate.admin()) {
            return Decision.skip(SkipReason.ADMIN);
        }

        if (!candidate.hasMemberOverride()) {
            return Decision.skip(SkipReason.NOT_IN_TICKET);
        }

        return Decision.apply();
    }

    public record Candidate(
            long userId,
            boolean hasTicketAccess,
            boolean hasMemberOverride,
            boolean admin
    ) {}

    public record Decision(boolean shouldApply, SkipReason reason) {
        public static Decision apply() {
            return new Decision(true, null);
        }

        public static Decision skip(SkipReason reason) {
            return new Decision(false, reason);
        }
    }

    public enum SkipReason {
        ALREADY_IN_TICKET,
        NOT_IN_TICKET,
        TICKET_INITIATOR,
        ADMIN
    }

    public record Summary(TicketMemberAction action, int applied, int skipped) {
        public String toUserMessage() {
            String verb = action == TicketMemberAction.ADD ? "adicionado(s)" : "removido(s)";

            if (applied == 0) {
                return skipped == 0
                        ? "> Nenhum membro selecionado."
                        : "> Nenhuma alteração feita. Seleções incompatíveis foram ignoradas.";
            }

            if (skipped == 0) {
                return String.format("> %d membro(s) %s com sucesso.", applied, verb);
            }

            return String.format(
                    "> %d membro(s) %s com sucesso. %d seleção(ões) ignorada(s).",
                    applied,
                    verb,
                    skipped
            );
        }
    }
}
