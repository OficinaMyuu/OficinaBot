package ofc.bot.handlers.games.mafia.service;

import ofc.bot.handlers.games.mafia.domain.DayResolution;
import ofc.bot.handlers.games.mafia.domain.MafiaMatch;
import ofc.bot.handlers.games.mafia.domain.MafiaPlayer;
import ofc.bot.handlers.games.mafia.domain.MafiaRoleConfiguration;
import ofc.bot.handlers.games.mafia.domain.NightInvestigationResult;
import ofc.bot.handlers.games.mafia.domain.NightResolution;
import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import ofc.bot.handlers.games.mafia.enums.MafiaTeam;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure rules engine for Oficina Dorme.
 * <p>
 * This class contains the role balancing, night/day resolution, and winner detection rules with no Discord-specific
 * side effects so the critical paths can be unit tested directly.
 */
public class MafiaMatchEngine {
    /**
     * Validates a manual staff-provided role configuration.
     *
     * @param cfg optional manual role configuration
     * @return validation error in pt-BR, or {@link Optional#empty()} when the configuration is acceptable
     */
    public Optional<String> validateRequestedConfiguration(@Nullable MafiaRoleConfiguration cfg) {
        if (cfg == null) {
            return Optional.empty();
        }

        if (cfg.assassins() < 2 || cfg.assassins() > 3) {
            return Optional.of("A quantidade de assassinos deve ficar entre 2 e 3.");
        }

        if (cfg.doctors() < 1 || cfg.doctors() > 2) {
            return Optional.of("A quantidade de médicos deve ficar entre 1 e 2.");
        }

        if (cfg.detectives() < 1 || cfg.detectives() > 2) {
            return Optional.of("A quantidade de detetives deve ficar entre 1 e 2.");
        }

        return Optional.empty();
    }

    /**
     * Resolves the final role configuration for a match, either from manual input or from auto-balance.
     *
     * @param requestedCfg optional manual configuration
     * @param playerCount total amount of players in the match
     * @return resolved role configuration
     * @throws IllegalArgumentException when the resolved configuration is invalid for the provided player count
     */
    public MafiaRoleConfiguration resolveConfiguration(@Nullable MafiaRoleConfiguration requestedCfg, int playerCount) {
        MafiaRoleConfiguration configuration = requestedCfg == null
                ? autoBalance(playerCount)
                : requestedCfg;
        Optional<String> validation = validateConfiguration(configuration, playerCount);
        if (validation.isPresent()) {
            throw new IllegalArgumentException(validation.get());
        }
        return configuration;
    }

    /**
     * Validates that a resolved role configuration can support a match with the given player count.
     *
     * @param configuration role configuration being validated
     * @param playerCount total amount of players in the match
     * @return validation error in pt-BR, or {@link Optional#empty()} when the configuration is valid
     */
    public Optional<String> validateConfiguration(MafiaRoleConfiguration configuration, int playerCount) {
        Optional<String> requestedValidation = validateRequestedConfiguration(configuration);
        if (requestedValidation.isPresent()) {
            return requestedValidation;
        }

        if (playerCount < MafiaMatch.MIN_PLAYERS) {
            return Optional.of("São necessários pelo menos 6 jogadores para iniciar a partida.");
        }

        int villagers = configuration.villagersFor(playerCount);
        if (villagers < 1) {
            return Optional.of("A configuração atual precisa deixar pelo menos 1 aldeão na partida.");
        }

        if (configuration.villageTeamFor(playerCount) <= configuration.assassins()) {
            return Optional.of("A partida precisa começar com mais membros da aldeia do que assassinos.");
        }

        return Optional.empty();
    }

    /**
     * Produces a best-effort automatic role configuration for the provided player count.
     *
     * @param playerCount total amount of players in the match
     * @return automatically balanced role configuration
     */
    public MafiaRoleConfiguration autoBalance(int playerCount) {
        int assassins = playerCount >= 10 ? 3 : 2;
        int doctors = playerCount >= 8 ? 2 : 1;
        int detectives = playerCount >= 9 ? 2 : 1;

        MafiaRoleConfiguration cfg = new MafiaRoleConfiguration(assassins, doctors, detectives);

        while (cfg.villagersFor(playerCount) < 1) {
            if (detectives > 1) {
                detectives--;
            } else if (doctors > 1) {
                doctors--;
            } else if (assassins > 2) {
                assassins--;
            } else {
                break;
            }

            cfg = new MafiaRoleConfiguration(assassins, doctors, detectives);
        }

        return cfg;
    }

    /**
     * Builds the randomized role deck used during role assignment.
     *
     * @param playerCount total amount of players in the match
     * @param cfg resolved role configuration
     * @return role deck sized to the player count
     */
    public List<MafiaRole> createRoleDeck(int playerCount, MafiaRoleConfiguration cfg) {
        List<MafiaRole> roles = new ArrayList<>(playerCount);

        for (int i = 0; i < cfg.assassins(); i++) {
            roles.add(MafiaRole.ASSASSIN);
        }

        for (int i = 0; i < cfg.doctors(); i++) {
            roles.add(MafiaRole.DOCTOR);
        }

        for (int i = 0; i < cfg.detectives(); i++) {
            roles.add(MafiaRole.DETECTIVE);
        }

        while (roles.size() < playerCount) {
            roles.add(MafiaRole.VILLAGER);
        }

        return roles;
    }

    /**
     * Resolves a full night round, applying doctor protection, assassin kills, and detective investigations.
     *
     * @param match active match in the night phase
     * @return immutable result of the resolved night
     */
    public NightResolution resolveNight(MafiaMatch match) {
        Set<Long> protectedFromInvestigation = new LinkedHashSet<>(match.getDoctorVotes().values());
        Set<Long> protectedFromDeath = resolveProtectedFromDeath(match);
        Set<Long> killedPlayers = resolveKilledPlayers(match, protectedFromDeath);
        applyNightDeaths(match, killedPlayers);
        Map<Long, NightInvestigationResult> investigations =
                resolveInvestigations(match, protectedFromInvestigation, killedPlayers);

        return new NightResolution(killedPlayers, investigations);
    }

    /**
     * Resolves a full day vote and determines whether somebody is eliminated or the result is a tie.
     *
     * @param dayVotes vote map keyed by voter id
     * @return immutable result of the resolved day vote
     */
    public DayResolution resolveDay(Map<Long, Long> dayVotes) {
        if (dayVotes.isEmpty()) {
            return new DayResolution(null, true, Map.of());
        }

        Map<Long, Integer> counters = new LinkedHashMap<>();
        for (long targetId : dayVotes.values()) {
            counters.merge(targetId, 1, Integer::sum);
        }

        int highestVotes = counters.values().stream().max(Integer::compareTo).orElse(0);
        List<Long> winners = counters.entrySet().stream()
                .filter(entry -> entry.getValue() == highestVotes)
                .map(Map.Entry::getKey)
                .toList();

        if (winners.size() != 1) {
            return new DayResolution(null, true, counters);
        }

        return new DayResolution(winners.getFirst(), false, counters);
    }

    /**
     * Determines whether the current alive roster already satisfies a victory condition.
     *
     * @param players current match players
     * @return winning team, or {@link Optional#empty()} when the match should continue
     */
    public Optional<MafiaTeam> determineWinner(Collection<MafiaPlayer> players) {
        long aliveAssassins = players.stream()
                .filter(MafiaPlayer::isAlive)
                .filter(player -> player.getRole() != null)
                .filter(player -> player.getRole().getTeam() == MafiaTeam.ASSASSINS)
                .count();

        long aliveVillage = players.stream()
                .filter(MafiaPlayer::isAlive)
                .filter(player -> player.getRole() != null)
                .filter(player -> player.getRole().getTeam() == MafiaTeam.VILLAGE)
                .count();

        if (aliveAssassins == 0) {
            return Optional.of(MafiaTeam.VILLAGE);
        }

        if (aliveVillage <= aliveAssassins) {
            return Optional.of(MafiaTeam.ASSASSINS);
        }

        return Optional.empty();
    }

    /**
     * Resolves which targets are protected from death this night.
     *
     * @param match active match
     * @return player ids protected from assassin kills
     */
    private Set<Long> resolveProtectedFromDeath(MafiaMatch match) {
        Set<Long> protectedFromDeath = new LinkedHashSet<>();

        for (Map.Entry<Long, Long> protection : match.getDoctorVotes().entrySet()) {
            if (!Objects.equals(protection.getKey(), protection.getValue())) {
                protectedFromDeath.add(protection.getValue());
            }
        }

        return protectedFromDeath;
    }

    /**
     * Resolves which players are killed by assassins after death protection is applied.
     *
     * @param match active match
     * @param protectedFromDeath player ids protected from assassin kills
     * @return killed player ids
     */
    private Set<Long> resolveKilledPlayers(MafiaMatch match, Set<Long> protectedFromDeath) {
        Set<Long> killedPlayers = new LinkedHashSet<>();

        for (long targetId : match.getAssassinVotes().values()) {
            if (!protectedFromDeath.contains(targetId)) {
                killedPlayers.add(targetId);
            }
        }

        return killedPlayers;
    }

    /**
     * Applies the resolved night deaths to the mutable match state.
     *
     * @param match active match
     * @param killedPlayers player ids resolved as killed this night
     */
    private void applyNightDeaths(MafiaMatch match, Set<Long> killedPlayers) {
        for (long targetId : killedPlayers) {
            MafiaPlayer player = match.getPlayer(targetId);
            if (player != null) {
                player.setAlive(false);
            }
        }
    }

    /**
     * Resolves detective investigations after protection and deaths are known.
     *
     * @param match active match
     * @param protectedFromInvestigation player ids protected from investigation
     * @param killedPlayers player ids killed this night
     * @return investigation results keyed by detective id
     */
    private Map<Long, NightInvestigationResult> resolveInvestigations(MafiaMatch match,
                                                                      Set<Long> protectedFromInvestigation,
                                                                      Set<Long> killedPlayers) {
        Map<Long, NightInvestigationResult> investigations = new LinkedHashMap<>();

        for (Map.Entry<Long, Long> investigation : match.getDetectiveVotes().entrySet()) {
            long detectiveId = investigation.getKey();
            long targetId = investigation.getValue();
            boolean blocked = protectedFromInvestigation.contains(targetId) || killedPlayers.contains(targetId);
            MafiaTeam revealedTeam = null;

            if (!blocked) {
                MafiaPlayer target = match.getPlayer(targetId);
                if (target != null && target.getRole() != null) {
                    revealedTeam = target.getRole().getTeam();
                }
            }

            investigations.put(detectiveId, new NightInvestigationResult(detectiveId, targetId, blocked, revealedTeam));
        }

        return investigations;
    }
}
