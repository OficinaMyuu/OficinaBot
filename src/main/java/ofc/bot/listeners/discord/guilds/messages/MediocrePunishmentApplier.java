package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class MediocrePunishmentApplier extends ListenerAdapter {
    private static final long USER_ID = 1199559648039153738L;

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        User author = e.getAuthor();

        if (author.getIdLong() != USER_ID || !test()) return;

        Bot.delete(e.getMessage());
    }

    private boolean test() {
        double mediocreChance = Bot.getMediocreChance();
        return Bot.chance(mediocreChance);
    }
}