package ofc.bot.listeners.discord.interactions;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import ofc.bot.domain.database.repository.UserPreferenceRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class GenericInteractionLocaleUpsert extends ListenerAdapter {
    private final UserPreferenceRepository usprefRepo;

    public GenericInteractionLocaleUpsert(UserPreferenceRepository usprefRepo) {
        this.usprefRepo = usprefRepo;
    }

    @Override
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        DiscordLocale userLocale = event.getUserLocale();
        User user = event.getUser();
        String locale = userLocale.getLocale();
        long userId = user.getIdLong();

        usprefRepo.setLocale(userId, locale);
    }
}
