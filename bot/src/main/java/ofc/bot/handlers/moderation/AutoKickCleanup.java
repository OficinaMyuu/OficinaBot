package ofc.bot.handlers.moderation;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.UserXPRepository;
import ofc.bot.handlers.economy.PaymentManager;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class AutoKickCleanup {
    private final UserXPRepository xpRepo;
    private final List<PaymentManager> paymentManagers;

    AutoKickCleanup(@NotNull UserXPRepository xpRepo, @NotNull List<PaymentManager> paymentManagers) {
        Checks.notNull(xpRepo, "UserXPRepository");
        Checks.notNull(paymentManagers, "Payment Managers");

        this.xpRepo = xpRepo;
        this.paymentManagers = List.copyOf(paymentManagers);
    }

    static AutoKickCleanup createDefault() {
        return new AutoKickCleanup(
                Repositories.getUserXPRepository(),
                List.of(
                        PaymentManagerProvider.getOficinaBankClient(),
                        PaymentManagerProvider.getUnbelievaBoatClient()
                )
        );
    }

    void reset(long userId, long guildId, @NotNull String reason) {
        Checks.notNull(reason, "Reason");

        xpRepo.deleteByUserId(userId);
        for (PaymentManager paymentManager : paymentManagers) {
            paymentManager.set(userId, guildId, 0, 0, reason);
        }
    }
}
