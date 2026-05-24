package ofc.bot.handlers.games.mafia.domain;

import ofc.bot.handlers.games.mafia.enums.MafiaTeam;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable result for a single detective investigation.
 *
 * @param detectiveId detective who investigated
 * @param targetId investigated target
 * @param blocked whether the investigation was blocked by protection or by the target dying that night
 * @param revealedTeam revealed team when the investigation succeeds, otherwise {@code null}
 */
public record NightInvestigationResult(long detectiveId, long targetId, boolean blocked,
                                       @Nullable MafiaTeam revealedTeam) {
}
