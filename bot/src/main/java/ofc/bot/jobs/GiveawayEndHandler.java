package ofc.bot.jobs;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.Main;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.database.repository.GiveawayRepository;
import ofc.bot.domain.database.repository.Repositories;
import ofc.bot.handlers.giveaway.GiveawayEndResult;
import ofc.bot.handlers.giveaway.GiveawayComponentFactory;
import ofc.bot.handlers.giveaway.GiveawayMessageFactory;
import ofc.bot.handlers.giveaway.GiveawayService;
import ofc.bot.handlers.giveaway.GiveawayServices;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@CronJob(expression = "0/15 * * ? * * *")
public class GiveawayEndHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayEndHandler.class);
    private final GiveawayService giveawayService;
    private final GiveawayRepository giveawayRepo;

    public GiveawayEndHandler() {
        this.giveawayService = GiveawayServices.create();
        this.giveawayRepo = Repositories.getGiveawayRepository();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<Giveaway> due = giveawayRepo.findDueActive(Bot.unixNow());
        for (Giveaway giveaway : due) {
            GiveawayEndResult result = giveawayService.endGiveaway(giveaway);
            if (result != null && result.changed()) {
                announce(result);
            }
        }
    }

    private void announce(GiveawayEndResult result) {
        JDA api = Main.getApi();
        TextChannel channel = api == null ? null : api.getTextChannelById(result.giveaway().getChannelId());
        if (channel == null) {
            return;
        }

        channel.sendMessageEmbeds(GiveawayMessageFactory.endedAnnouncement(result.giveaway(), result.winners(), false))
                .setComponents(GiveawayComponentFactory.requiresClaim(result.giveaway()) && !result.winners().isEmpty()
                        ? List.of(ActionRow.of(GiveawayComponentFactory.claimButton(result.giveaway().getGiveawayId())))
                        : List.of())
                .queue();
    }
}
