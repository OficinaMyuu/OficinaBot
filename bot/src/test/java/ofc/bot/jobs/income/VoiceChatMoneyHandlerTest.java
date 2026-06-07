package ofc.bot.jobs.income;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoiceChatMoneyHandlerTest {
    @Test
    void shouldParseConfiguredBankChannelIds() {
        Set<Long> channelIds = VoiceChatMoneyHandler.parseBankChannelIds(new String[]{
                "1065077982588305538",
                "1414389901415419915",
                "1065077982588305538",
                "not-a-snowflake"
        });

        assertEquals(Set.of(1065077982588305538L, 1414389901415419915L), channelIds);
    }

    @Test
    void shouldPayNormalVoiceIncomeToCash() {
        VoiceChatMoneyHandler.VoiceIncomePayout payout = VoiceChatMoneyHandler.calculatePayout(25, false);

        assertEquals(25, payout.cash());
        assertEquals(0, payout.bank());
        assertEquals(25, payout.total());
    }

    @Test
    void shouldPayBankVoiceIncomeWithMultiplier() {
        VoiceChatMoneyHandler.VoiceIncomePayout payout = VoiceChatMoneyHandler.calculatePayout(25, true);

        assertEquals(0, payout.cash());
        assertEquals(50, payout.bank());
        assertEquals(50, payout.total());
    }
}
