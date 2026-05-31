package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.entities.Guild;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.sqlite.repository.AccumulatorPrizeRepository;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
import ofc.bot.domain.sqlite.repository.ColorRoleStateRepository;
import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.economy.PaymentManager;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AccumulatorPayoutService {
    private final AccumulatorPrizeRepository prizeRepo;
    private final ColorRoleItemRepository colorItemRepo;
    private final ColorRoleStateRepository colorStateRepo;
    private final Function<CurrencyType, PaymentManager> paymentProvider;
    private final AccumulatorDiscordBridge discordBridge;

    public AccumulatorPayoutService(
            AccumulatorPrizeRepository prizeRepo,
            ColorRoleItemRepository colorItemRepo,
            ColorRoleStateRepository colorStateRepo
    ) {
        this(
                prizeRepo,
                colorItemRepo,
                colorStateRepo,
                PaymentManagerProvider::fromType,
                new JdaAccumulatorDiscordBridge()
        );
    }

    public AccumulatorPayoutService(
            AccumulatorPrizeRepository prizeRepo,
            ColorRoleItemRepository colorItemRepo,
            ColorRoleStateRepository colorStateRepo,
            Function<CurrencyType, PaymentManager> paymentProvider,
            AccumulatorDiscordBridge discordBridge
    ) {
        this.prizeRepo = prizeRepo;
        this.colorItemRepo = colorItemRepo;
        this.colorStateRepo = colorStateRepo;
        this.paymentProvider = paymentProvider;
        this.discordBridge = discordBridge;
    }

    public synchronized AccumulatorApprovalReport approveOne(@NotNull Guild guild, int prizeId, long approverId) {
        return approveOne(guild.getIdLong(), guild, prizeId, approverId);
    }

    public synchronized AccumulatorApprovalReport approveOne(long guildId, Guild guild, int prizeId, long approverId) {
        AccumulatorPrize prize = prizeRepo.findPendingById(guildId, prizeId);
        if (prize == null) {
            return AccumulatorApprovalReport.failure(1, 0, "Prize not found or already processed.", List.of());
        }
        return approve(guildId, guild, List.of(prize), approverId);
    }

    public synchronized AccumulatorApprovalReport approveAll(@NotNull Guild guild, long approverId) {
        return approveAll(guild.getIdLong(), guild, approverId);
    }

    public synchronized AccumulatorApprovalReport approveAll(long guildId, Guild guild, long approverId) {
        List<AccumulatorPrize> prizes = prizeRepo.findAllPending(guildId);
        return approve(guildId, guild, prizes, approverId);
    }

    private AccumulatorApprovalReport approve(long guildId, Guild guild, List<AccumulatorPrize> prizes, long approverId) {
        long startNanos = System.nanoTime();
        List<Integer> ids = ids(prizes);

        if (prizes.isEmpty()) {
            return failure(startNanos, 0, "There are no pending prizes to approve.", List.of());
        }

        List<String> validationErrors = validate(guild, prizes);
        if (!validationErrors.isEmpty()) {
            String error = String.join("\n", validationErrors);
            prizeRepo.saveLastError(guildId, ids, Bot.limitStr(error, 500), Bot.unixNow());
            return failure(startNanos, prizes.size(), "Nothing was paid because the pending box is not ready.", validationErrors);
        }

        List<Runnable> rollbacks = new ArrayList<>();
        try {
            for (AccumulatorPrize prize : prizes) {
                rollbacks.add(executePrize(guildId, guild, prize));
            }

            long now = Bot.unixNow();
            int updated = prizeRepo.markPaid(guildId, ids, approverId, now);
            if (updated != prizes.size()) {
                throw new IllegalStateException("Could not mark every prize as paid.");
            }

            return AccumulatorApprovalReport.success(
                    prizes.size(),
                    elapsed(startNanos),
                    "Paid " + prizes.size() + " accumulated prize(s).",
                    List.of("Approved by <@" + approverId + ">.")
            );
        } catch (RuntimeException e) {
            List<String> details = new ArrayList<>();
            details.add("Payment failed: " + e.getMessage());
            details.addAll(rollback(rollbacks));
            prizeRepo.saveLastError(guildId, ids, Bot.limitStr(String.join("\n", details), 500), Bot.unixNow());
            return failure(startNanos, prizes.size(), "Nothing was marked as paid.", details);
        }
    }

    private List<String> validate(Guild guild, List<AccumulatorPrize> prizes) {
        List<String> errors = new ArrayList<>();

        for (AccumulatorPrize prize : prizes) {
            if (!discordBridge.memberExists(guild, prize.getTargetId())) {
                errors.add("#" + prize.getId() + ": member <@" + prize.getTargetId() + "> was not found.");
                continue;
            }

            if (prize.getType() == AccumulatorPrizeType.MONEY) {
                validateMoney(prize, errors);
            } else {
                validateColorRole(guild, prize, errors);
            }
        }
        return errors;
    }

    private void validateMoney(AccumulatorPrize prize, List<String> errors) {
        Integer amount = prize.getAmount();
        if (!AccumulatorInputParser.isValidAmount(amount)) {
            errors.add("#" + prize.getId() + ": invalid money amount.");
        }

        if (prize.getCurrency() == null) {
            errors.add("#" + prize.getId() + ": choose a currency before approving.");
        }
    }

    private void validateColorRole(Guild guild, AccumulatorPrize prize, List<String> errors) {
        Long roleId = prize.getColorRoleId();
        if (roleId == null) {
            errors.add("#" + prize.getId() + ": choose a color role before approving.");
            return;
        }

        if (colorItemRepo.findByRoleId(roleId) == null) {
            errors.add("#" + prize.getId() + ": selected role is not a registered color role.");
        }

        if (!discordBridge.roleExists(guild, roleId)) {
            errors.add("#" + prize.getId() + ": selected color role no longer exists.");
        }

        Long duration = prize.getColorDurationSeconds();
        if (duration == null || duration <= 0) {
            errors.add("#" + prize.getId() + ": invalid color role duration.");
        }
    }

    private Runnable executePrize(long guildId, Guild guild, AccumulatorPrize prize) {
        return switch (prize.getType()) {
            case MONEY -> executeMoney(guildId, prize);
            case COLOR_ROLE -> executeColorRole(guildId, guild, prize);
        };
    }

    private Runnable executeMoney(long guildId, AccumulatorPrize prize) {
        CurrencyType currency = prize.getCurrency();
        Integer amount = prize.getAmount();
        if (currency == null || amount == null) {
            throw new IllegalStateException("Prize #" + prize.getId() + " is not configured.");
        }

        PaymentManager payment = paymentProvider.apply(currency);
        BankAccount account = payment.update(
                prize.getTargetId(),
                guildId,
                0,
                amount,
                "Accumulator prize #" + prize.getId()
        );

        if (account == null) {
            throw new IllegalStateException("Economy update failed for prize #" + prize.getId() + ".");
        }

        return () -> payment.update(
                prize.getTargetId(),
                guildId,
                0,
                -amount,
                "Rollback of accumulator prize #" + prize.getId()
        );
    }

    private Runnable executeColorRole(long guildId, Guild guild, AccumulatorPrize prize) {
        long userId = prize.getTargetId();
        long roleId = prize.getColorRoleId();
        long duration = prize.getColorDurationSeconds();
        long now = Bot.unixNow();
        ColorRoleState existing = colorStateRepo.findByUserAndRoleId(userId, roleId);
        boolean hadRole = discordBridge.memberHasRole(guild, userId, roleId);
        Long oldExpiresAt = existing == null ? null : existing.getExpiresAt();
        Long oldUpdatedAt = existing == null ? null : existing.getLastUpdated();
        boolean addedRole = false;

        try {
            if (!hadRole) {
                discordBridge.addRole(guild, userId, roleId);
                addedRole = true;
            }

            if (existing == null) {
                ColorRoleState state = new ColorRoleState(
                        0,
                        CurrencyType.OFICINA,
                        userId,
                        guildId,
                        roleId,
                        now + duration,
                        now,
                        now
                );
                colorStateRepo.save(state);
            } else {
                long base = Math.max(now, existing.getExpiresAt());
                existing.setExpiresAt(base + duration).setLastUpdated(now);
                colorStateRepo.upsert(existing);
            }
        } catch (RuntimeException e) {
            if (addedRole) {
                try {
                    discordBridge.removeRole(guild, userId, roleId);
                } catch (RuntimeException ignored) {
                }
            }
            throw e;
        }

        boolean shouldRemoveRole = addedRole;
        return () -> {
            if (existing == null) {
                colorStateRepo.deleteByGuildUserAndRoleId(guildId, userId, roleId);
            } else {
                existing.setExpiresAt(oldExpiresAt).setLastUpdated(oldUpdatedAt);
                colorStateRepo.upsert(existing);
            }

            if (shouldRemoveRole) {
                discordBridge.removeRole(guild, userId, roleId);
            }
        };
    }

    private List<String> rollback(List<Runnable> rollbacks) {
        List<String> failures = new ArrayList<>();

        for (int i = rollbacks.size() - 1; i >= 0; i--) {
            try {
                rollbacks.get(i).run();
            } catch (RuntimeException e) {
                failures.add("Rollback failed: " + e.getMessage());
            }
        }
        return failures;
    }

    private AccumulatorApprovalReport failure(long startNanos, int requested, String summary, List<String> details) {
        return AccumulatorApprovalReport.failure(requested, elapsed(startNanos), summary, details);
    }

    private long elapsed(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private List<Integer> ids(List<AccumulatorPrize> prizes) {
        return prizes.stream()
                .map(AccumulatorPrize::getId)
                .toList();
    }
}
