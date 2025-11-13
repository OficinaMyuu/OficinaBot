package ofc.bot.jobs.income;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import ofc.bot.Main;
import ofc.bot.handlers.TemporalTaskExecutor;
import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.PaymentManagerProvider;
import ofc.bot.handlers.economy.unb.UnbelievaBoatClient;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@CronJob(expression = "0 0/5 * ? * * *")
public class VoiceChatMoneyHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceChatMoneyHandler.class);
    private static final Random random = new Random();
    private static final List<Long> SPECIAL_CHANNEL_IDS = List.of(1065077982588305538L, 1414389901415419915L, 693627612454453250L);
    private static final int MIN_VALUE = 20;
    private static final int MAX_VALUE = 40;
    private final UnbelievaBoatClient paymentManager = PaymentManagerProvider.getUnbelievaBoatClient();

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Guild> guilds = Main.getApi().getGuilds();
        List<Member> membersToPay = VoiceIncomeUtil.getEligibleMembers(guilds);
        TemporalTaskExecutor executor = new TemporalTaskExecutor(1000); // 1 request per second
        int totalGiven = 0;

        if (membersToPay.isEmpty()) return;

        for (Member member : membersToPay) {
            Guild guild = member.getGuild();
            int randomValue = random.nextInt(MIN_VALUE, MAX_VALUE + 1);
            long userId = member.getIdLong();
            long currentVoiceChannelId = member.getVoiceState().getChannel().getIdLong();
            long guildId = guild.getIdLong();
            boolean isSpecial = SPECIAL_CHANNEL_IDS.contains(currentVoiceChannelId);

            int amount = isSpecial ? randomValue * 2 : randomValue;
            executor.addTask(() -> {
                // "Special" cases are members in the Salada voice channel, which
                // earn 2x the amount of money and get credited in their bank,
                // instead of cash for normal voice channels
                long cash = isSpecial ? 0 : amount;
                long bank = isSpecial ? amount : 0;

                BankAccount balance = paymentManager.update(userId, guildId, cash, bank, "VoiceChat money");
                if (balance == null)
                    LOGGER.warn("Failed to give money to user '{}'", userId);
            });
            totalGiven += amount;
        }
        LOGGER.info("A total of ${} was given to {} different members",
                String.format("%02d", totalGiven), membersToPay.size()
        );
        executor.run();
    }
}