package ofc.bot.handlers.giveaway;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.GiveawayEntry;
import ofc.bot.domain.entity.GiveawayWinner;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.domain.entity.enums.GiveawayStatus;
import ofc.bot.domain.entity.enums.GiveawayWinnerStatus;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.domain.sqlite.repository.GiveawayEntryRepository;
import ofc.bot.domain.sqlite.repository.GiveawayRepository;
import ofc.bot.domain.sqlite.repository.GiveawayWinnerRepository;
import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.economy.PaymentManager;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GiveawayService {
    private final GiveawayRepository giveawayRepo;
    private final GiveawayEntryRepository entryRepo;
    private final GiveawayWinnerRepository winnerRepo;
    private final ColorRoleItemRepository colorItemRepo;
    private final ColorRoleStateRepository colorStateRepo;
    private final GiveawayWinnerSelector winnerSelector;
    private final GiveawayMessageUpdater messageUpdater;

    public GiveawayService(
            GiveawayRepository giveawayRepo,
            GiveawayEntryRepository entryRepo,
            GiveawayWinnerRepository winnerRepo,
            ColorRoleItemRepository colorItemRepo,
            ColorRoleStateRepository colorStateRepo,
            GiveawayWinnerSelector winnerSelector,
            GiveawayMessageUpdater messageUpdater
    ) {
        this.giveawayRepo = giveawayRepo;
        this.entryRepo = entryRepo;
        this.winnerRepo = winnerRepo;
        this.colorItemRepo = colorItemRepo;
        this.colorStateRepo = colorStateRepo;
        this.winnerSelector = winnerSelector;
        this.messageUpdater = messageUpdater;
    }

    public GiveawayEntryResult enter(@NotNull String giveawayId, @NotNull Member member) {
        Giveaway giveaway = giveawayRepo.findById(giveawayId);
        if (giveaway == null) {
            return GiveawayEntryResult.GIVEAWAY_NOT_FOUND;
        }

        if (!giveaway.isActive() || giveaway.getEndsAt() <= Bot.unixNow()) {
            return GiveawayEntryResult.GIVEAWAY_ENDED;
        }

        if (member.getUser().isBot()) {
            return GiveawayEntryResult.BOT_NOT_ALLOWED;
        }

        Long requiredVoiceChannelId = giveaway.getRequiredVoiceChannelId();
        if (requiredVoiceChannelId != null && !Bot.isInVoiceChannel(member, requiredVoiceChannelId)) {
            return GiveawayEntryResult.MUST_BE_IN_VOICE_CHANNEL;
        }

        boolean added = entryRepo.addEntry(giveawayId, member.getIdLong(), Bot.unixNow());
        if (!added) {
            return GiveawayEntryResult.ALREADY_ENTERED;
        }

        messageUpdater.post(giveawayId);
        return GiveawayEntryResult.ENTERED;
    }

    public Giveaway findGiveaway(@NotNull String giveawayId) {
        return giveawayRepo.findById(giveawayId);
    }

    public GiveawayWinner findActiveWinner(@NotNull String giveawayId, long userId) {
        return winnerRepo.findActiveWinner(giveawayId, userId);
    }

    public List<ColorRoleItem> findAvailableColorRoles(@NotNull Guild guild) {
        return colorItemRepo.findAll().stream()
                .filter(item -> guild.getRoleById(item.getRoleId()) != null)
                .sorted(Comparator.comparing(
                        item -> guild.getRoleById(item.getRoleId()).getName(),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    public GiveawayEntryResult leave(@NotNull String giveawayId, long userId) {
        Giveaway giveaway = giveawayRepo.findById(giveawayId);
        if (giveaway == null) {
            return GiveawayEntryResult.GIVEAWAY_NOT_FOUND;
        }

        if (!giveaway.isActive()) {
            return GiveawayEntryResult.GIVEAWAY_ENDED;
        }

        boolean removed = entryRepo.removeEntry(giveawayId, userId);
        if (!removed) {
            return GiveawayEntryResult.NOT_ENTERED;
        }

        messageUpdater.post(giveawayId);
        return GiveawayEntryResult.LEFT;
    }

    public int removeVoiceLockedEntries(long userId, long voiceChannelId) {
        List<String> giveawayIds = entryRepo.findActiveGiveawaysForVoiceChannel(voiceChannelId);
        int removed = 0;

        for (String giveawayId : giveawayIds) {
            if (entryRepo.removeEntry(giveawayId, userId)) {
                removed++;
                messageUpdater.post(giveawayId);
            }
        }
        return removed;
    }

    public GiveawayEndResult endGiveaway(@NotNull Giveaway giveaway) {
        long now = Bot.unixNow();
        boolean changed = giveawayRepo.markEnded(giveaway.getGiveawayId(), now);
        Giveaway persisted = giveawayRepo.findById(giveaway.getGiveawayId());
        if (!changed && persisted != null) {
            return new GiveawayEndResult(
                    persisted,
                    entryRepo.countByGiveaway(giveaway.getGiveawayId()),
                    winnerRepo.findActiveByGiveaway(giveaway.getGiveawayId()),
                    false
            );
        }

        List<Long> entryIds = entryRepo.findByGiveaway(giveaway.getGiveawayId())
                .stream()
                .map(GiveawayEntry::getUserId)
                .toList();
        List<Long> winnerIds = winnerSelector.selectWinners(entryIds, giveaway.getWinnerCount(), Set.of());
        GiveawayWinnerStatus winnerStatus = winnerStatusFor(giveaway.getPrizeType());
        winnerRepo.saveWinners(giveaway.getGiveawayId(), winnerIds, winnerStatus, now);

        Giveaway ended = giveawayRepo.findById(giveaway.getGiveawayId());
        List<GiveawayWinner> winners = winnerRepo.findActiveByGiveaway(giveaway.getGiveawayId());
        messageUpdater.refreshNow(giveaway.getGiveawayId());

        return new GiveawayEndResult(ended, entryIds.size(), winners, true);
    }

    public GiveawayEndResult endGiveaway(@NotNull String identifier) {
        Giveaway giveaway = giveawayRepo.findByIdOrMessageId(identifier);
        if (giveaway == null) {
            return null;
        }
        return endGiveaway(giveaway);
    }

    public GiveawayEndResult reroll(@NotNull String identifier, int winnerCount) {
        Giveaway giveaway = giveawayRepo.findByIdOrMessageId(identifier);
        if (giveaway == null || giveaway.isActive()) {
            return null;
        }

        String giveawayId = giveaway.getGiveawayId();
        long now = Bot.unixNow();
        Set<Long> excluded = winnerRepo.findAllWinnerIds(giveawayId).stream().collect(Collectors.toSet());
        List<Long> entryIds = entryRepo.findByGiveaway(giveawayId)
                .stream()
                .map(GiveawayEntry::getUserId)
                .toList();
        List<Long> winnerIds = winnerSelector.selectWinners(entryIds, winnerCount, excluded);

        if (!winnerIds.isEmpty()) {
            winnerRepo.markActiveAsRerolled(giveawayId, now);
            winnerRepo.saveWinners(giveawayId, winnerIds, winnerStatusFor(giveaway.getPrizeType()), now);
        }

        List<GiveawayWinner> winners = winnerRepo.findActiveByGiveaway(giveawayId);
        messageUpdater.refreshNow(giveawayId);
        return new GiveawayEndResult(giveaway, entryIds.size(), winners, true);
    }

    public GiveawayClaimResult claimMoney(@NotNull String giveawayId, @NotNull Member member, @NotNull CurrencyType currency) {
        Giveaway giveaway = giveawayRepo.findById(giveawayId);
        if (giveaway == null) {
            return GiveawayClaimResult.GIVEAWAY_NOT_FOUND;
        }

        if (giveaway.getPrizeType() != GiveawayPrizeType.ECONOMY_MONEY) {
            return GiveawayClaimResult.WRONG_PRIZE_TYPE;
        }

        GiveawayWinner winner = winnerRepo.findActiveWinner(giveawayId, member.getIdLong());
        if (winner == null) {
            return GiveawayClaimResult.NOT_A_WINNER;
        }

        if (winner.getStatus() != GiveawayWinnerStatus.PENDING_CLAIM) {
            return GiveawayClaimResult.NOT_CLAIMABLE;
        }

        long now = Bot.unixNow();
        if (!winnerRepo.startClaim(giveawayId, member.getIdLong(), now)) {
            return GiveawayClaimResult.ALREADY_CLAIMING;
        }

        try {
            PaymentManager bank = PaymentManagerProvider.fromType(currency);
            long amount = giveaway.getMoneyAmount() == null ? 0 : giveaway.getMoneyAmount();
            BankAccount account = bank.update(
                    member.getIdLong(),
                    member.getGuild().getIdLong(),
                    0,
                    amount,
                    "Prêmio de sorteio " + giveawayId
            );

            if (account == null) {
                winnerRepo.markFailed(giveawayId, member.getIdLong(), Bot.unixNow());
                return GiveawayClaimResult.ECONOMY_FAILURE;
            }

            winnerRepo.markClaimed(giveawayId, member.getIdLong(), currency, null, Bot.unixNow());
            return GiveawayClaimResult.CLAIMED;
        } catch (RuntimeException e) {
            winnerRepo.markFailed(giveawayId, member.getIdLong(), Bot.unixNow());
            return GiveawayClaimResult.ECONOMY_FAILURE;
        }
    }

    public GiveawayClaimResult claimColorRole(@NotNull String giveawayId, @NotNull Member member, long roleId) {
        Giveaway giveaway = giveawayRepo.findById(giveawayId);
        if (giveaway == null) {
            return GiveawayClaimResult.GIVEAWAY_NOT_FOUND;
        }

        if (giveaway.getPrizeType() != GiveawayPrizeType.COLOR_ROLE) {
            return GiveawayClaimResult.WRONG_PRIZE_TYPE;
        }

        GiveawayWinner winner = winnerRepo.findActiveWinner(giveawayId, member.getIdLong());
        if (winner == null) {
            return GiveawayClaimResult.NOT_A_WINNER;
        }

        if (winner.getStatus() != GiveawayWinnerStatus.PENDING_CLAIM) {
            return GiveawayClaimResult.NOT_CLAIMABLE;
        }

        ColorRoleItem color = colorItemRepo.findByRoleId(roleId);
        if (color == null) {
            return GiveawayClaimResult.INVALID_COLOR_ROLE;
        }

        Guild guild = member.getGuild();
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            return GiveawayClaimResult.ROLE_NOT_FOUND;
        }

        long now = Bot.unixNow();
        if (!winnerRepo.startClaim(giveawayId, member.getIdLong(), now)) {
            return GiveawayClaimResult.ALREADY_CLAIMING;
        }

        try {
            guild.addRoleToMember(member, role).complete();

            long duration = giveaway.getColorRoleDurationSeconds() == null
                    ? ColorRoleState.DEFAULT_DURATION_SECONDS
                    : giveaway.getColorRoleDurationSeconds();
            ColorRoleState state = new ColorRoleState(
                    0,
                    CurrencyType.OFICINA,
                    member.getIdLong(),
                    guild.getIdLong(),
                    roleId,
                    now + duration,
                    Bot.nowMillis(),
                    now
            );
            colorStateRepo.save(state);
            winnerRepo.markClaimed(giveawayId, member.getIdLong(), null, roleId, Bot.unixNow());
            return GiveawayClaimResult.CLAIMED;
        } catch (RuntimeException e) {
            winnerRepo.markFailed(giveawayId, member.getIdLong(), Bot.unixNow());
            return GiveawayClaimResult.DISCORD_FAILURE;
        }
    }

    private GiveawayWinnerStatus winnerStatusFor(GiveawayPrizeType prizeType) {
        return prizeType == GiveawayPrizeType.GENERIC
                ? GiveawayWinnerStatus.MANUAL_FULFILLMENT
                : GiveawayWinnerStatus.PENDING_CLAIM;
    }
}
