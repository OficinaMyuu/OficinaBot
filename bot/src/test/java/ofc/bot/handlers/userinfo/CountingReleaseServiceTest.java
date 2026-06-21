package ofc.bot.handlers.userinfo;

import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.BankAction;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.economy.PaymentManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountingReleaseServiceTest {
    @Test
    void shouldChargeSelectedEconomyBankOnly() {
        RecordingPaymentManager payment = new RecordingPaymentManager(CurrencyType.UNBELIEVABOAT, true);
        CountingReleaseService service = new CountingReleaseService(currency -> payment);

        CountingReleaseService.ReleaseAttempt attempt = service.charge(
                CurrencyType.UNBELIEVABOAT,
                42L,
                100L,
                true
        );

        assertEquals(CountingReleaseService.Result.CHARGED, attempt.result());
        assertNotNull(attempt.action());
        assertEquals(List.of(new ChargeCall(
                42L,
                100L,
                0L,
                CountingReleaseService.PRICE,
                CountingReleaseService.PAYMENT_REASON
        )), payment.calls);
    }

    @Test
    void shouldNotChargeWhenMemberIsAlreadyReleased() {
        RecordingPaymentManager payment = new RecordingPaymentManager(CurrencyType.OFICINA, true);
        CountingReleaseService service = new CountingReleaseService(currency -> payment);

        CountingReleaseService.ReleaseAttempt attempt = service.charge(CurrencyType.OFICINA, 42L, 100L, false);

        assertEquals(CountingReleaseService.Result.ALREADY_RELEASED, attempt.result());
        assertNull(attempt.action());
        assertTrue(payment.calls.isEmpty());
    }

    @Test
    void shouldReportInsufficientBalanceWhenChargeFails() {
        RecordingPaymentManager payment = new RecordingPaymentManager(CurrencyType.OFICINA, false);
        CountingReleaseService service = new CountingReleaseService(currency -> payment);

        CountingReleaseService.ReleaseAttempt attempt = service.charge(CurrencyType.OFICINA, 42L, 100L, true);

        assertEquals(CountingReleaseService.Result.INSUFFICIENT_BALANCE, attempt.result());
        assertNull(attempt.action());
        assertEquals(1, payment.calls.size());
    }

    @Test
    void shouldShowReleaseButtonOnlyForSelfWithPunishmentRole() {
        CountingReleaseService service = new CountingReleaseService(currency -> {
            throw new AssertionError("Payment provider should not be used for visibility checks.");
        });

        assertTrue(service.shouldShowReleaseButton(42L, 42L, true));
        assertFalse(service.shouldShowReleaseButton(42L, 42L, false));
        assertFalse(service.shouldShowReleaseButton(7L, 42L, true));
    }

    private record ChargeCall(long userId, long guildId, long cash, long bank, String reason) {}

    private static final class RecordingPaymentManager implements PaymentManager {
        private final CurrencyType currencyType;
        private final boolean chargeSucceeds;
        private final List<ChargeCall> calls = new ArrayList<>();

        private RecordingPaymentManager(CurrencyType currencyType, boolean chargeSucceeds) {
            this.currencyType = currencyType;
            this.chargeSucceeds = chargeSucceeds;
        }

        @Override
        public BankAccount get(long userId, long guildId) {
            return null;
        }

        @Override
        public BankAccount set(long userId, long guildId, long cash, long bank, String reason) {
            return null;
        }

        @Override
        public BankAccount update(long userId, long guildId, long cash, long bank, String reason) {
            return null;
        }

        @Override
        public CurrencyType getCurrencyType() {
            return currencyType;
        }

        @Override
        public BankAction charge(long userId, long guildId, long cash, long bank, String reason) {
            calls.add(new ChargeCall(userId, guildId, cash, bank, reason));
            return chargeSucceeds
                    ? BankAction.STATIC_SUCCESS_NO_CHANGE
                    : BankAction.STATIC_FAILURE_NO_CHANGE;
        }
    }
}
