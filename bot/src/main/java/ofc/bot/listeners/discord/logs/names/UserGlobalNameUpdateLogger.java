package ofc.bot.listeners.discord.logs.names;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.UserNameUpdate;
import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.sqlite.repository.UserNameUpdateRepository;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class UserGlobalNameUpdateLogger extends ListenerAdapter {
    private final UserNameUpdateRepository namesRepo;
    private final UserRepository usersRepo;

    public UserGlobalNameUpdateLogger(UserNameUpdateRepository namesRepo, UserRepository usersRepo) {
        this.namesRepo = namesRepo;
        this.usersRepo = usersRepo;
    }

    @Override
    public void onUserUpdateGlobalName(UserUpdateGlobalNameEvent event) {
        User user = event.getUser();
        String newName = event.getNewGlobalName();
        String oldName = event.getOldGlobalName();
        long userId = user.getIdLong();

        UserNameUpdate update = new UserNameUpdate(
                userId, userId, null,
                NameScope.GLOBAL_NAME, oldName, newName, Bot.unixNow()
        );

        usersRepo.upsert(AppUser.fromUser(user));
        namesRepo.save(update);
    }
}