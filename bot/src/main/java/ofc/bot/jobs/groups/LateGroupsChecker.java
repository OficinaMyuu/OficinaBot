package ofc.bot.jobs.groups;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.RentStatus;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.util.content.Channels;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@CronJob(expression = "59 59 23 L * ? *")
public class LateGroupsChecker implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(LateGroupsChecker.class);
    private final OficinaGroupRepository grpRepo;

    public LateGroupsChecker() {
        this.grpRepo = Repositories.getOficinaGroupRepository();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TextChannel chan = Channels.LATE_GROUPS_INVOICE.textChannel();
        if (chan == null) {
            LOGGER.warn("Could not find text channel for id {}", Channels.LATE_GROUPS_INVOICE.fetchId());
            return;
        }

        grpRepo.updateGroupsStatus(RentStatus.NOT_PAID, RentStatus.PENDING);
        List<OficinaGroup> groups = grpRepo.findByRentStatus(RentStatus.NOT_PAID);
        if (groups.isEmpty()) return;

        String msg = String.format("Os grupos: [%s] não pagaram o aluguel até hoje.", formatGroups(groups));
        chan.sendMessage(msg).queue();
    }

    private String formatGroups(List<OficinaGroup> groups) {
        return groups.stream()
                .map(g -> String.format("`%s`", g.getName()))
                .collect(Collectors.joining(", "));
    }
}