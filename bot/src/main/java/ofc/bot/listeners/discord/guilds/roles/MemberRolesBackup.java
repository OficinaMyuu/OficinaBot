package ofc.bot.listeners.discord.guilds.roles;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.FormerMemberRole;
import ofc.bot.domain.sqlite.repository.FormerMemberRoleRepository;
import ofc.bot.domain.sqlite.repository.UserXPRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Roles;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The job responsible for deleting expired backups is at
 * {@link ofc.bot.jobs.roles.ExpiredBackupsRemover ExpiredBackupsRemover}.
 */
@DiscordEventHandler
public class MemberRolesBackup extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemberRolesBackup.class);
    private static final long ANCHOR_LEVEL = 10;
    private static final long TARGET_GUILD = 582430782577049600L;
    private final FormerMemberRoleRepository rolesRepo;
    private final UserXPRepository xpRepo;

    public MemberRolesBackup(FormerMemberRoleRepository rolesRepo, UserXPRepository xpRepo) {
        this.rolesRepo = rolesRepo;
        this.xpRepo = xpRepo;
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (member == null || guild.getIdLong() != TARGET_GUILD) return;

        Member self = guild.getSelfMember();
        boolean privileged = isPrivileged(member);
        boolean shouldPersist = isMemberIncluded(member) || privileged;

        if (!shouldPersist)
            return;

        List<Role> savedRoles = member.getRoles()
                .stream()
                .filter(self::canInteract)
                .toList();

        saveRoles(member, savedRoles, privileged);
    }

    private void saveRoles(Member member, List<Role> roles, boolean privileged) {
        String username = member.getUser().getName();
        long userId = member.getIdLong();
        long guildId = member.getGuild().getIdLong();

        List<FormerMemberRole> savedRoles = new ArrayList<>(roles.size());
        for (Role r : roles) {
            FormerMemberRole rec = new FormerMemberRole(userId, guildId, r.getIdLong(), privileged, Bot.unixNow());
            savedRoles.add(rec);
        }

        try {
            int saveCount = rolesRepo.bulkSave(savedRoles);
            LOGGER.info("Successfully stored {} roles of @{}", saveCount, username);
        } catch (DataAccessException e) {
            LOGGER.error("Could not save {} roles of {}", roles.size(), userId, e);
        }
    }

    /*
     * Privileged members are members who will have their roles persisted forever,
     * the ExpiredBackupsRemover#EXPIRY_DAYS will not affect their backed-up roles.
     */
    private boolean isPrivileged(Member member) {
        List<Role> roles = member.getRoles();
        Role salada = Roles.SALADA.role();

        if (Staff.isStaff(member)) return true;

        return salada != null && roles.contains(salada);
    }

    /*
     * This method checks whether the given Member should have its roles backed up
     * in the database according to their level, or if Member#isBoosting() returns true.
     */
    private boolean isMemberIncluded(Member member) {
        return member.isBoosting() || hasExpectedLevel(member.getIdLong());
    }

    private boolean hasExpectedLevel(long userId) {
        return xpRepo.findLevelByUserId(userId) >= ANCHOR_LEVEL;
    }
}