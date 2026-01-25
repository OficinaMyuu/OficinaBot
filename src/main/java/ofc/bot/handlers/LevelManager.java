package ofc.bot.handlers;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.entity.LevelRole;
import ofc.bot.domain.entity.UserXP;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Channels;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class LevelManager {
    private static LevelManager instance;
    private final UserXPRepository xpRepo;
    private final LevelRoleRepository lvlRoleRepo;
    private final AppUserBanRepository appBanRepo;
    private final Set<Long> policyCache;

    private LevelManager() {
        this.xpRepo = Repositories.getUserXPRepository();
        this.lvlRoleRepo = Repositories.getLevelRoleRepository();
        this.appBanRepo = Repositories.getAppUserBanRepository();
        this.policyCache = Repositories.getEntityPolicyRepository().findSetByType(PolicyType.BLOCK_XP_GAINS, Long::parseLong);
    }

    public static LevelManager getManager() {
        if (instance == null) instance = new LevelManager();
        return instance;
    }

    public synchronized void addXp(@NotNull Member member, @NotNull GuildChannel channel, int xp) {
        Checks.notNull(member, "Member");
        Checks.notNull(channel, "Channel");
        Checks.positive(xp, "Xp");
        Guild guild = member.getGuild();
        long userId = member.getIdLong();
        long chanId = channel.getIdLong();

        if (appBanRepo.isBanned(userId) || isExcluded(member, chanId)) return;

        UserXP userXp = xpRepo.findByUserId(userId, UserXP.fromUserId(userId));
        int xpMod = userXp.getXp() + xp;
        int oldLevel = userXp.getLevel();

        UserXP.compute(xpMod, oldLevel, (newXp, newLevel) -> userXp.setXp(newXp)
                .setLevel(newLevel)
                .tickUpdate());
        xpRepo.upsert(userXp);

        // If the levels after the patch are not the same (indicating a level up),
        // we check for roles to be given and notify the user in the respective channel.
        int currLevel = userXp.getLevel();
        if (currLevel <= oldLevel) return;

        TextChannel chan = Channels.LEVEL_UP.textChannel();
        if (chan != null) {
            chan.sendMessageFormat("%s avançou para o nível %d!", member.getAsMention(), currLevel).queue();
        }

        // Check if we should give the user a new role for their rank.
        // If the closest role to this given level is not found or does not match
        // the level the user is currently on, we ignore it.
        LevelRole levelRole = lvlRoleRepo.findLastByLevel(currLevel);
        if (levelRole == null || levelRole.getLevel() != currLevel) return;
        guild.addRoleToMember(member, levelRole.toRole()).queue();

        LevelRole oldLvlRole = lvlRoleRepo.findLastByLevel(oldLevel);
        if (oldLvlRole != null) {
            // We could check for all the other available level roles
            // and use "Guild#modifyMemberRoles()" to remove them all at once,
            // but its not necessary, removing the old role is enough.
            guild.removeRoleFromMember(member, oldLvlRole.toRole()).queue();
        }
    }

    /**
     * Merges multiple UserXP instances into a single result.
     * Useful for combining accounts or stats.
     * @param xps One or more UserXP objects to merge.
     * @return A new UserXP object (without ID) containing the combined progress.
     */
    public UserXP merge(@NotNull UserXP... xps) {
        if (xps == null || xps.length == 0) {
            return new UserXP(0, 0, 0L, Bot.unixNow(), Bot.unixNow());
        }

        long grandTotalXp = 0;
        for (UserXP userXp : xps) {
            if (userXp == null) continue;
            grandTotalXp += getAbsoluteXp(userXp);
        }

        // We create a fresh object. ID is 0 because this is a merged result.
        UserXP result = new UserXP(0, 0, 0L, Bot.unixNow(), Bot.unixNow());

        // We cap at Integer.MAX_VALUE because your UserXP class uses 'int' for XP.
        // If you expect massive merges, you might need to migrate UserXP to 'long'.
        int cappedTotal = (int) Math.min(grandTotalXp, Integer.MAX_VALUE);

        // We reuse your existing compute logic.
        // passing '0' as currentLevel forces it to calculate from scratch.
        UserXP.compute(cappedTotal, 0, (newXp, newLevel) -> {
            result.setXp(newXp);
            result.setLevel(newLevel);
        });

        return result;
    }

    /**
     * Helper to calculate the total XP a user has earned since Level 0.
     */
    public long getAbsoluteXp(UserXP userXp) {
        long total = userXp.getXp(); // Start with current partial XP

        // Sum the XP required for every level the user has already passed
        for (int i = 0; i < userXp.getLevel(); i++) {
            total += UserXP.calcNextXp(i);
        }
        return total;
    }

    private boolean isExcluded(Member member, long chanId) {
        return policyCache.contains(chanId) || member.getRoles()
                .stream()
                .anyMatch(r -> policyCache.contains(r.getIdLong()));
    }
}