package ofc.bot.jobs.income;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.VoiceChannelIncomeRuleRepository;
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

/**
 * Scheduled job that grants automatic money to eligible voice-channel members.
 */
@CronJob(expression = "0 0/5 * ? * * *")
public class VoiceChatMoneyHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceChatMoneyHandler.class);
    private static final Random random = new Random();
    private static final String BANK_CHANNEL_IDS_KEY = "income.voice.bank-channel-ids";
    private static final VoiceChannelIncomePayoutType PAYOUT_TYPE = VoiceChannelIncomePayoutType.MONEY;
    private static final ExecutorService PAYOUT_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("voice-income-", 0).factory()
    );
    private static final AtomicBoolean PAYOUT_RUNNING = new AtomicBoolean(false);
    private static final int MIN_VALUE = 20;
    private static final int MAX_VALUE = 40;
    private final UnbelievaBoatClient paymentManager = PaymentManagerProvider.getUnbelievaBoatClient();
    private final AutomatedMoneyGainPolicy gainPolicy = new AutomatedMoneyGainPolicy();
    private final VoiceChannelIncomeRuleRepository ruleRepo;

    /**
     * Creates the job with the default income rule repository.
     */
    public VoiceChatMoneyHandler() {
        this(Repositories.getVoiceChannelIncomeRuleRepository());
    }

    /**
     * Creates the job with an explicit income rule repository.
     */
    VoiceChatMoneyHandler(VoiceChannelIncomeRuleRepository ruleRepo) {
        this.ruleRepo = ruleRepo;
    }

    /**
     * Loads voice members and dispatches the money payout cycle.
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        VoiceIncomeRuleCache rules = getRuleCache();
        List<Guild> guilds = Main.getApi().getGuilds();
        List<Member> membersToPay = VoiceIncomeUtil.getEligibleMembers(guilds, rules, PAYOUT_TYPE);

        if (membersToPay.isEmpty()) return;

        if (!PAYOUT_RUNNING.compareAndSet(false, true)) {
            LOGGER.warn("Skipping voice chat income payout because the previous payout cycle is still running.");
            return;
        }

        PAYOUT_EXECUTOR.execute(() -> {
            try {
                payMembers(membersToPay, rules);
            } catch (Exception e) {
                LOGGER.error("Unexpected failure while paying voice chat income.", e);
            } finally {
                PAYOUT_RUNNING.set(false);
            }
        });
    }

    /**
     * Pays every eligible member using the rules cached for this job run.
     */
    private void payMembers(List<Member> membersToPay, VoiceIncomeRuleCache rules) {
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
            double multiplier = rules.multiplierFor(currentVoiceChannelId, PAYOUT_TYPE);

            if (gainPolicy.isBlocked(member, currentVoiceChannelId)) continue;

            VoiceIncomePayout payout = calculatePayout(randomValue, paysBank, multiplier);
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

    /**
     * Loads the money rules once for the current scheduled cycle.
     */
    private VoiceIncomeRuleCache getRuleCache() {
        return VoiceIncomeRuleCache.from(ruleRepo.findByPayoutType(PAYOUT_TYPE));
    }

    /**
     * Reads configured voice channels that should receive income in bank.
     */
    private static Set<Long> getBankChannelIds() {
        return parseBankChannelIds(Bot.getArray(BANK_CHANNEL_IDS_KEY));
    }

    /**
     * Parses bank-channel ids from database-backed bot config values.
     */
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

    /**
     * Calculates the default money payout without a custom multiplier.
     */
    static VoiceIncomePayout calculatePayout(int baseAmount, boolean paysBank) {
        return calculatePayout(baseAmount, paysBank, 1.0D);
    }

    /**
     * Calculates the money payout after bank routing and custom multiplier.
     */
    static VoiceIncomePayout calculatePayout(int baseAmount, boolean paysBank, double multiplier) {
        int bankAdjustedAmount = paysBank ? baseAmount * 2 : baseAmount;
        int total = VoiceIncomeUtil.scalePayout(bankAdjustedAmount, multiplier);
        long cash = paysBank ? 0 : total;
        long bank = paysBank ? total : 0;

        return new VoiceIncomePayout(cash, bank, total);
    }

    /**
     * Value object describing the final cash and bank update.
     */
    record VoiceIncomePayout(long cash, long bank, int total) {}
}
