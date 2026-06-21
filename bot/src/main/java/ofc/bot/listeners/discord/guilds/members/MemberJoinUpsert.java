package ofc.bot.listeners.discord.guilds.members;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.MemberJoinEvent;
import ofc.bot.domain.sqlite.repository.MemberJoinEventRepository;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class MemberJoinUpsert extends ListenerAdapter {
    private final MemberJoinEventRepository joinEventRepo;
    private final UserRepository userRepo;

    public MemberJoinUpsert(MemberJoinEventRepository joinEventRepo, UserRepository userRepo) {
        this.joinEventRepo = joinEventRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        User user = event.getUser();
        long joinedAt = event.getMember()
                .getTimeJoined()
                .toEpochSecond();

        userRepo.upsert(AppUser.fromUser(user));
        joinEventRepo.save(new MemberJoinEvent(
                event.getGuild().getIdLong(),
                user.getIdLong(),
                joinedAt
        ));
    }
}
