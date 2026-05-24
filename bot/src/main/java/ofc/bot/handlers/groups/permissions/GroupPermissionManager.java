package ofc.bot.handlers.groups.permissions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.domain.entity.EntityPolicy;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.GroupPermission;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.entity.enums.ResourceType;
import ofc.bot.domain.sqlite.repository.EntityPolicyRepository;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GroupPermissionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupPermissionManager.class);
    private final EntityPolicyRepository policyRepo;

    public GroupPermissionManager(EntityPolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    public boolean grant(GroupPermission permission, OficinaGroup group) {
        try {
            if (permission.isDiscord()) {
                discordGrant(permission.getReferencedPermission(), group);
            } else {
                applicationGrant(permission.getPolicyType(), group);
            }
            return true;
        } catch (DataAccessException e) {
            LOGGER.error("Could not grant permission {} to group {}", permission, group.getId(), e);
            return false;
        }
    }

    public boolean revoke(GroupPermission permission, OficinaGroup group) {
        try {
            if (permission.isDiscord()) {
                discordRevoke(permission.getReferencedPermission(), group);
            } else {
                applicationRevoke(permission.getPolicyType(), group);
            }
            return true;
        } catch (DataAccessException e) {
            LOGGER.error("Could not revoke permission {} from group {}", permission, group.getId(), e);
            return false;
        }
    }

    public boolean isGranted(GroupPermission permission, OficinaGroup group) {
        return permission.isDiscord()
                ? isDiscordGranted(permission.getReferencedPermission(), group)
                : isApplicationGranted(permission.getPolicyType(), group);
    }

    public static List<GroupPermission> findAll() {
        return List.of(GroupPermission.values());
    }

    private void discordGrant(Permission perm, OficinaGroup group) {
        TextChannel chan = group.getTextChannel();
        Role role = group.resolveRole();
        if (chan == null || role == null) return;

        chan.upsertPermissionOverride(role)
                .grant(perm)
                .complete();
    }

    private void applicationGrant(PolicyType policy, OficinaGroup group) {
        TextChannel chan = group.getTextChannel();
        if (chan == null) return;

        EntityPolicy entityPolicy = new EntityPolicy(policy, ResourceType.CHANNEL, chan.getId());
        policyRepo.save(entityPolicy);
    }

    private void discordRevoke(Permission perm, OficinaGroup group) {
        TextChannel chan = group.getTextChannel();
        Role role = group.resolveRole();
        if (chan == null || role == null) return;

        PermissionOverride override = chan.getPermissionOverride(role);
        if (override != null) {
            override.getManager().clear(perm).complete();
        }
    }

    private void applicationRevoke(PolicyType policy, OficinaGroup group) {
        TextChannel chan = group.getTextChannel();
        if (chan == null) return;

        policyRepo.deleteByPolicyAndResource(policy, chan.getId());
    }

    private boolean isDiscordGranted(Permission perm, OficinaGroup group) {
        TextChannel chan = group.getTextChannel();
        Role role = group.resolveRole();
        if (chan == null || role == null) return false;

        PermissionOverride override = chan.getPermissionOverride(role);
        return override != null && override.getAllowed().contains(perm);
    }

    private boolean isApplicationGranted(PolicyType policy, OficinaGroup group) {
        TextChannel chan = group.getTextChannel();
        if (chan == null) return false;

        return policyRepo.existsByTypeAndResource(policy, chan.getId());
    }
}