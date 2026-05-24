package ofc.bot.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.Main;
import ofc.bot.domain.entity.TempBan;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.TempBanRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

@CronJob(expression = "0 * * ? * * *")
public class ExpiredBanHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredBanHandler.class);
    private static final Consumer<Throwable> DEFAULT_ERR_HANDLER = (e) -> LOGGER.error("Could not revoke ban", e);
    private final TempBanRepository tmpBanRepo;

    public ExpiredBanHandler() {
        this.tmpBanRepo = Repositories.getTempBanRepository();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long now = Bot.unixNow();
        JDA api = Main.getApi();
        List<TempBan> banlist = tmpBanRepo.findFromBefore(now);

        if (banlist.isEmpty()) return;

        // Pretty printing ^^
        if (banlist.size() == 1) {
            LOGGER.info("Found 1 temporary ban to be revoked ");
        } else {
            LOGGER.info("Found {} temporary bans to be revoked ", banlist.size());
        }

        for (TempBan ban : banlist) {
            long userId = ban.getUserId();
            long guildId = ban.getGuildId();
            Guild guild = api.getGuildById(guildId);

            if (guild == null) {
                LOGGER.info("Guild for id {} not found", guildId);
                continue;
            }

            guild.unban(User.fromId(userId))
                    .reason("Expired temporary ban")
                    .queue(null, DEFAULT_ERR_HANDLER);
        }
        tmpBanRepo.deleteIn(banlist);
    }
}