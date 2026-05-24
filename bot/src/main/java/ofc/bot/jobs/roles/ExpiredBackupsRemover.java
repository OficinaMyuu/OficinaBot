package ofc.bot.jobs.roles;

import ofc.bot.domain.sqlite.repository.FormerMemberRoleRepository;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.tables.FormerMembersRolesTable;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.jooq.exception.DataAccessException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for removing backups older than {@value #EXPIRY_DAYS} days
 * from the database (table {@link FormerMembersRolesTable FormerMembersRoles}) based on the
 * {@link FormerMembersRolesTable#CREATED_AT created_at} column.
 */
@CronJob(expression = "0 0 0 ? * * *") // Every day at midnight
public class ExpiredBackupsRemover implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredBackupsRemover.class);
    private static final long EXPIRY_DAYS = 90;
    private final FormerMemberRoleRepository rolesRepo;

    public ExpiredBackupsRemover() {
        this.rolesRepo = Repositories.getFormerMemberRoleRepository();
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Removing expired backups (older than {} days)...", TimeUnit.SECONDS.toDays(EXPIRY_DAYS));

        try {
            int deleteCount = rolesRepo.deleteBefore(90, TimeUnit.DAYS, false);

            if (deleteCount == 0)
                LOGGER.info("No backups were deleted");
            else
                LOGGER.info("Successfully deleted a total of {} backed-up roles", deleteCount);
        } catch (DataAccessException e) {
            LOGGER.error("Could not delete expired backups", e);
        }
    }
}