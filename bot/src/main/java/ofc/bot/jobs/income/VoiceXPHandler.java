package ofc.bot.jobs.income;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
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

@CronJob(expression = "0 0/5 * ? * * *")
public class VoiceXPHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoiceXPHandler.class);
    private static final Random RANDOM = new Random();
    private static final int MIN_VALUE = 12;
    private static final int MAX_VALUE = 40;
    private final LevelManager levelManager;

    public VoiceXPHandler() {
        this.levelManager = LevelManager.getManager();
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Looking for members to receive XP for voice-channel presence...");
        List<Guild> guilds = Main.getApi().getGuilds();
        List<Member> members = VoiceIncomeUtil.getEligibleMembers(guilds);

        if (members.size() == 1) {
            LOGGER.info("Found 1 member");
        } else {
            LOGGER.info("Found {} members", members.size());
        }
        if (members.isEmpty()) return;

        for (Member member : members) {
            int xp = RANDOM.nextInt(MIN_VALUE, MAX_VALUE + 1);
            GuildChannel chan = member.getVoiceState().getChannel();

            levelManager.addXp(member, chan, xp);
        }
    }
}