package ofc.bot.listeners.discord.guilds.members;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class MemberJoinUpsert extends ListenerAdapter {
    private final UserRepository userRepo;

    public MemberJoinUpsert() {
        this.userRepo = Repositories.getUserRepository();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        User user = event.getUser();
        userRepo.upsert(AppUser.fromUser(user));
    }
}