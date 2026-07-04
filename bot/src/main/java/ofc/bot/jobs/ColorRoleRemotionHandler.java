package ofc.bot.jobs;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.Main;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.domain.database.repository.ColorRoleStateRepository;
import ofc.bot.domain.database.repository.Repositories;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.jobs.CronJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@CronJob(expression = "0 0 0 ? * * *") // Every day at midnight
public class ColorRoleRemotionHandler implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorRoleRemotionHandler.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("Checking for members with color role due to be removed...");

        ColorRoleStateRepository repository = Repositories.getColorRoleStateRepository();
        List<ColorRoleState> toRemove = repository.findExpired(Bot.unixNow());
        JDA api = Main.getApi();

        if (toRemove.isEmpty()) {
            LOGGER.info("No members found.");
            return;
        }

        LOGGER.info("Found {} member{}.", toRemove.size(), toRemove.size() == 1 ? "" : "s");

        for (ColorRoleState data : toRemove) {
            processExpiredColorRole(api, repository, data);
        }
    }

    void processExpiredColorRole(JDA api, ColorRoleStateRepository repository, ColorRoleState data) {
        long guildId = data.getGuildId();
        long roleId = data.getRoleId();
        Guild guild = api.getGuildById(guildId);

        if (guild == null) {
            LOGGER.warn("Could not find guild with id {}! Ignoring color role removal", guildId);
            return;
        }

        Role role = guild.getRoleById(roleId);
        if (role == null) {
            deleteStaleColorRole(repository, data);
            return;
        }

        removeRoleFromMember(guild, role, repository, data);
    }

    private void removeRoleFromMember(
            Guild guild,
            Role role,
            ColorRoleStateRepository repository,
            ColorRoleState data
    ) {
        long guildId = data.getGuildId();
        long userId = data.getUserId();
        long roleId = data.getRoleId();

        guild.removeRoleFromMember(UserSnowflake.fromId(userId), role).queue((s) -> {
            LOGGER.info("Successfully removed color role {} from {}", roleId, userId);
            removeFrom(repository, guildId, userId, roleId);
        }, (error) -> {
            if (error instanceof ErrorResponseException response && response.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) {
                removeFrom(repository, guildId, userId, roleId);
                LOGGER.warn("Member '{}' was not found, probably no longer present in the server, ignoring.", userId);
                return;
            }

            LOGGER.error("Could not remove color role {} from {}", roleId, userId, error);
        });
    }

    private void deleteStaleColorRole(ColorRoleStateRepository repository, ColorRoleState data) {
        long guildId = data.getGuildId();
        long userId = data.getUserId();
        long roleId = data.getRoleId();

        int deletedRows = removeFrom(repository, guildId, userId, roleId);
        LOGGER.warn(
                "Could not find role {} in guild {}! Deleted {} stale color role row{} from the database.",
                roleId,
                guildId,
                deletedRows,
                deletedRows == 1 ? "" : "s"
        );
    }

    private int removeFrom(ColorRoleStateRepository repository, long guildId, long userId, long roleId) {
        return repository.deleteByGuildUserAndRoleId(guildId, userId, roleId);
    }
}
