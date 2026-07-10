package ofc.bot.handlers.userinfo;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.economy.BankAction;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.economy.PaymentManager;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import ofc.bot.domain.database.repository.StoreItemSettingsRepository;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;

public class CountingReleaseService {
    public static final String ROLE_CONFIG_KEY = "fun.counting.punishments.role.id";
    public static final String PAYMENT_REASON = "Counting punishment release";
    private final Function<CurrencyType, PaymentManager> paymentProvider;
    private final Supplier<OptionalInt> priceProvider;

    public CountingReleaseService(@NotNull StoreItemSettingsRepository storeItemSettingsRepo) {
        this(
                () -> storeItemSettingsRepo.findPrice(StoreItemType.COUNTING_RELEASE),
                PaymentManagerProvider::fromType
        );
    }

    CountingReleaseService(
            @NotNull Supplier<OptionalInt> priceProvider,
            @NotNull Function<CurrencyType, PaymentManager> paymentProvider
    ) {
        Checks.notNull(paymentProvider, "Payment Provider");
        Checks.notNull(priceProvider, "Price Provider");
        this.paymentProvider = paymentProvider;
        this.priceProvider = priceProvider;
    }

    public long getPunishmentRoleId() {
        return Bot.getSafe(ROLE_CONFIG_KEY, Long::parseLong);
    }

    public OptionalLong findPunishmentRoleId() {
        try {
            return OptionalLong.of(getPunishmentRoleId());
        } catch (RuntimeException e) {
            return OptionalLong.empty();
        }
    }

    public boolean shouldShowReleaseButton(long issuerId, long targetId, boolean targetHasPunishmentRole) {
        return issuerId == targetId && targetHasPunishmentRole;
    }

    @NotNull
    public OptionalInt findPrice() {
        return priceProvider.get();
    }

    public ReleaseAttempt charge(
            @NotNull CurrencyType currency,
            long userId,
            long guildId,
            boolean hasPunishmentRole
    ) {
        Checks.notNull(currency, "Currency");

        if (!hasPunishmentRole) {
            return ReleaseAttempt.alreadyReleased();
        }

        OptionalInt configuredPrice = findPrice();
        if (configuredPrice.isEmpty()) {
            return ReleaseAttempt.configurationUnavailable();
        }

        PaymentManager payment = paymentProvider.apply(currency);
        BankAction action = payment.charge(userId, guildId, 0, configuredPrice.getAsInt(), PAYMENT_REASON);

        return action.isOk()
                ? ReleaseAttempt.charged(action)
                : ReleaseAttempt.insufficientBalance();
    }

    public record ReleaseAttempt(Result result, BankAction action) {
        public static ReleaseAttempt charged(BankAction action) {
            return new ReleaseAttempt(Result.CHARGED, action);
        }

        public static ReleaseAttempt insufficientBalance() {
            return new ReleaseAttempt(Result.INSUFFICIENT_BALANCE, null);
        }

        public static ReleaseAttempt alreadyReleased() {
            return new ReleaseAttempt(Result.ALREADY_RELEASED, null);
        }

        public static ReleaseAttempt configurationUnavailable() {
            return new ReleaseAttempt(Result.CONFIGURATION_UNAVAILABLE, null);
        }
    }

    public enum Result {
        CHARGED,
        INSUFFICIENT_BALANCE,
        ALREADY_RELEASED,
        CONFIGURATION_UNAVAILABLE
    }
}
