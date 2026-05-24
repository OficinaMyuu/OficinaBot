package ofc.bot.listeners.discord.logs.names;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.UserNameUpdate;
import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.sqlite.repository.UserNameUpdateRepository;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class UserNameUpdateLogger extends ListenerAdapter {
    private final UserNameUpdateRepository namesRepo;
    private final UserRepository usersRepo;

    public UserNameUpdateLogger(UserNameUpdateRepository namesRepo, UserRepository usersRepo) {
        this.namesRepo = namesRepo;
        this.usersRepo = usersRepo;
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        User user = event.getUser();
        String newName = event.getNewName();
        String oldName = event.getOldName();
        long userId = user.getIdLong();

        UserNameUpdate update = new UserNameUpdate(
                userId, userId, null,
                NameScope.USERNAME, oldName, newName, Bot.unixNow()
        );

        usersRepo.upsert(AppUser.fromUser(user));
        namesRepo.save(update);
    }
}