package ofc.bot.handlers.games.mafia.domain;

import java.util.Map;
import java.util.Set;

/**
 * Immutable result of a resolved night round.
 *
 * @param killedPlayerIds players killed after protection is applied
 * @param investigationsByDetective detective results keyed by detective id
 */
public record NightResolution(Set<Long> killedPlayerIds, Map<Long, NightInvestigationResult> investigationsByDetective) {
}
