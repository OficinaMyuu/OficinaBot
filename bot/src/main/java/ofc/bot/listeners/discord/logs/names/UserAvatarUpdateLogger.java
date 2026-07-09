package ofc.bot.listeners.discord.logs.names;

import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.database.repository.UserRepository;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class UserAvatarUpdateLogger extends ListenerAdapter {
    private final UserRepository usersRepo;

    public UserAvatarUpdateLogger(UserRepository usersRepo) {
        this.usersRepo = usersRepo;
    }

    @Override
    public void onUserUpdateAvatar(UserUpdateAvatarEvent event) {
        usersRepo.upsert(AppUser.fromUser(event.getUser()));
    }
}
