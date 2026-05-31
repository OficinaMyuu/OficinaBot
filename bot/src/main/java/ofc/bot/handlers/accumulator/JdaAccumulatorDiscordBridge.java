package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class JdaAccumulatorDiscordBridge implements AccumulatorDiscordBridge {
    @Override
    public boolean memberExists(Guild guild, long userId) {
        return findMember(guild, userId) != null;
    }

    @Override
    public boolean roleExists(Guild guild, long roleId) {
        return guild.getRoleById(roleId) != null;
    }

    @Override
    public boolean memberHasRole(Guild guild, long userId, long roleId) {
        Member member = findMember(guild, userId);
        Role role = guild.getRoleById(roleId);
        return member != null && role != null && member.getRoles().contains(role);
    }

    @Override
    public void addRole(Guild guild, long userId, long roleId) {
        Member member = requireMember(guild, userId);
        Role role = requireRole(guild, roleId);
        guild.addRoleToMember(member, role).complete();
    }

    @Override
    public void removeRole(Guild guild, long userId, long roleId) {
        Member member = requireMember(guild, userId);
        Role role = requireRole(guild, roleId);
        guild.removeRoleFromMember(member, role).complete();
    }

    private Member requireMember(Guild guild, long userId) {
        Member member = findMember(guild, userId);
        if (member == null) {
            throw new IllegalStateException("Membro não encontrado: " + userId);
        }
        return member;
    }

    private Role requireRole(Guild guild, long roleId) {
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            throw new IllegalStateException("Cargo não encontrado: " + roleId);
        }
        return role;
    }

    private Member findMember(Guild guild, long userId) {
        Member cached = guild.getMemberById(userId);
        if (cached != null) {
            return cached;
        }

        try {
            return guild.retrieveMemberById(userId).complete();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
