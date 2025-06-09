package ofc.bot.listeners.discord.logs.names;

import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.UserNameUpdate;
import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.sqlite.repository.UserNameUpdateRepository;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The reason why we do not use the {@link net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent GuildMemberUpdateNicknameEvent} here
 * is because Discord does not tell us who changed their nickname.
 */
@DiscordEventHandler
public class MemberNickUpdateLogger extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MemberNickUpdateLogger.class);
    private final UserNameUpdateRepository namesRepo;
    private final UserRepository usersRepo;

    public MemberNickUpdateLogger(UserNameUpdateRepository namesRepo, UserRepository usersRepo) {
        this.namesRepo = namesRepo;
        this.usersRepo = usersRepo;
    }

    @Override
    public void onGuildAuditLogEntryCreate(GuildAuditLogEntryCreateEvent event) {
        Guild guild = event.getGuild();
        AuditLogEntry entry = event.getEntry();
        AuditLogChange change = entry.getChangeByKey("nick");

        if (entry.getType() != ActionType.MEMBER_UPDATE || change == null) return;

        String newValue = change.getNewValue();
        String oldValue = change.getOldValue();
        long createdAt = entry.getTimeCreated().toEpochSecond();
        long targetId = entry.getTargetIdLong();
        long moderatorId = entry.getUserIdLong();
        long guildId = guild.getIdLong();

        Bot.fetchUser(targetId).queue(target -> {
            UserNameUpdate editMapping = new UserNameUpdate(
                    targetId, moderatorId, guildId,
                    NameScope.GUILD_NICK, oldValue, newValue, createdAt
            );

            usersRepo.upsert(AppUser.fromUser(target));
            namesRepo.save(editMapping);
        }, e -> {
            // This is one of those scenarios that is unlikely (impossible) to happen
            // but better safe than sorry?
            LOGGER.warn("User '{}' was not found for nickname logging", targetId);
        });
    }
}