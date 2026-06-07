package ofc.bot.jobs.income;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import ofc.bot.Main;
import ofc.bot.handlers.economy.AutomatedMoneyGainPolicy;
import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import ofc.bot.handlers.economy.unb.UnbelievaBoatClient;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@CronJob(expression = "0 0/5 * ? * * *")
public class VoiceChatMoneyHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceChatMoneyHandler.class);
    private static final Random random = new Random();
    private static final String BANK_CHANNEL_IDS_KEY = "income.voice.bank-channel-ids";
    private static final ExecutorService PAYOUT_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("voice-income-", 0).factory()
    );
    private static final AtomicBoolean PAYOUT_RUNNING = new AtomicBoolean(false);
    private static final int MIN_VALUE = 20;
    private static final int MAX_VALUE = 40;
    private final UnbelievaBoatClient paymentManager = PaymentManagerProvider.getUnbelievaBoatClient();
    private final AutomatedMoneyGainPolicy gainPolicy = new AutomatedMoneyGainPolicy();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Guild> guilds = Main.getApi().getGuilds();
        List<Member> membersToPay = VoiceIncomeUtil.getEligibleMembers(guilds);

        if (membersToPay.isEmpty()) return;

        if (!PAYOUT_RUNNING.compareAndSet(false, true)) {
            LOGGER.warn("Skipping voice chat income payout because the previous payout cycle is still running.");
            return;
        }

        PAYOUT_EXECUTOR.execute(() -> {
            try {
                payMembers(membersToPay);
            } catch (Exception e) {
                LOGGER.error("Unexpected failure while paying voice chat income.", e);
            } finally {
                PAYOUT_RUNNING.set(false);
            }
        });
    }

    private void payMembers(List<Member> membersToPay) {
        Set<Long> bankChannelIds = getBankChannelIds();
        int totalGiven = 0;
        int paidMembers = 0;

        for (Member member : membersToPay) {
            Guild guild = member.getGuild();
            if (member.getVoiceState() == null || member.getVoiceState().getChannel() == null) continue;

            int randomValue = random.nextInt(MIN_VALUE, MAX_VALUE + 1);
            long userId = member.getIdLong();
            long currentVoiceChannelId = member.getVoiceState().getChannel().getIdLong();
            long guildId = guild.getIdLong();
            boolean paysBank = bankChannelIds.contains(currentVoiceChannelId);

            if (gainPolicy.isBlocked(member, currentVoiceChannelId)) continue;

            VoiceIncomePayout payout = calculatePayout(randomValue, paysBank);
            BankAccount balance = paymentManager.update(userId, guildId, payout.cash(), payout.bank(), "VoiceChat money");
            if (balance == null)
                LOGGER.warn("Failed to give money to user '{}'", userId);

            totalGiven += payout.total();
            paidMembers++;
        }

        if (paidMembers == 0) return;

        LOGGER.info("A total of ${} was given to {} different members",
                String.format("%02d", totalGiven), paidMembers
        );
    }

    private static Set<Long> getBankChannelIds() {
        return parseBankChannelIds(Bot.getArray(BANK_CHANNEL_IDS_KEY));
    }

    static Set<Long> parseBankChannelIds(String[] rawChannelIds) {
        Set<Long> channelIds = new HashSet<>();
        for (String rawChannelId : rawChannelIds) {
            try {
                channelIds.add(Long.parseLong(rawChannelId));
            } catch (NumberFormatException e) {
                LOGGER.warn("Ignoring invalid voice income bank channel id '{}'", rawChannelId);
            }
        }
        return channelIds;
    }

    static VoiceIncomePayout calculatePayout(int baseAmount, boolean paysBank) {
        int total = paysBank ? baseAmount * 2 : baseAmount;
        long cash = paysBank ? 0 : total;
        long bank = paysBank ? total : 0;

        return new VoiceIncomePayout(cash, bank, total);
    }

    record VoiceIncomePayout(long cash, long bank, int total) {}
}
