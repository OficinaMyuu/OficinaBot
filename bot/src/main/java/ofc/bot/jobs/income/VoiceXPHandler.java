package ofc.bot.jobs.income;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import ofc.bot.domain.entity.enums.VoiceChannelIncomePayoutType;
import ofc.bot.domain.database.repository.Repositories;
import ofc.bot.domain.database.repository.VoiceChannelIncomeRuleRepository;
import ofc.bot.Main;
import ofc.bot.handlers.LevelManager;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Scheduled job that grants automatic XP to eligible voice-channel members.
 */
@CronJob(expression = "0 0/5 * ? * * *")
public class VoiceXPHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceXPHandler.class);
    private static final Random RANDOM = new Random();
    private static final VoiceChannelIncomePayoutType PAYOUT_TYPE = VoiceChannelIncomePayoutType.LEVEL_EXPERIENCE;
    private static final int MIN_VALUE = 12;
    private static final int MAX_VALUE = 40;
    private final LevelManager levelManager;
    private final VoiceChannelIncomeRuleRepository ruleRepo;

    /**
     * Creates the job with the default level manager and income rule repository.
     */
    public VoiceXPHandler() {
        this(LevelManager.getManager(), Repositories.getVoiceChannelIncomeRuleRepository());
    }

    /**
     * Creates the job with explicit collaborators.
     */
    VoiceXPHandler(LevelManager levelManager, VoiceChannelIncomeRuleRepository ruleRepo) {
        this.levelManager = levelManager;
        this.ruleRepo = ruleRepo;
    }

    /**
     * Loads voice members and applies the XP payout cycle.
     */
    @Override
    @SuppressWarnings("DataFlowIssue")
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Looking for members to receive XP for voice-channel presence...");
        VoiceIncomeRuleCache rules = getRuleCache();
        List<Guild> guilds = Main.getApi().getGuilds();
        List<Member> members = VoiceIncomeUtil.getEligibleMembers(guilds, rules, PAYOUT_TYPE);

        if (members.size() == 1) {
            LOGGER.info("Found 1 member");
        } else {
            LOGGER.info("Found {} members", members.size());
        }
        if (members.isEmpty()) return;

        for (Member member : members) {
            GuildChannel chan = member.getVoiceState().getChannel();
            double multiplier = rules.multiplierFor(chan.getIdLong(), PAYOUT_TYPE);
            int xp = calculateXp(RANDOM.nextInt(MIN_VALUE, MAX_VALUE + 1), multiplier);

            levelManager.addXp(member, chan, xp);
        }
    }

    /**
     * Calculates the final XP amount after a custom multiplier.
     */
    static int calculateXp(int baseAmount, double multiplier) {
        return VoiceIncomeUtil.scalePayout(baseAmount, multiplier);
    }

    /**
     * Loads the XP rules once for the current scheduled cycle.
     */
    private VoiceIncomeRuleCache getRuleCache() {
        return VoiceIncomeRuleCache.from(ruleRepo.findByPayoutType(PAYOUT_TYPE));
    }
}
