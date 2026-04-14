package ofc.bot.listeners.discord.guilds.mafia;

import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.handlers.games.mafia.service.MafiaGameManager;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

/**
 * Watches guild lifecycle events that can invalidate an active Oficina Dorme match.
 * <p>
 * The listener reacts to channel/thread deletion and member departures so the match can be stopped or updated
 * immediately instead of waiting for somebody to discover the broken state manually.
 */
@DiscordEventHandler
public class MafiaLifecycleListener extends ListenerAdapter {
    private final MafiaGameManager gameManager = MafiaGameManager.getInstance();

    /**
     * Terminates a match when one of its required channels is deleted.
     *
     * @param event Discord channel deletion event
     */
    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        gameManager.handleChannelDeleted(event.getJDA(), event.getGuild(), event.getChannel().getIdLong());
    }

    /**
     * Removes a player from active matches when they leave or are kicked from the guild.
     *
     * @param event guild member removal event
     */
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        gameManager.handlePlayerUnavailable(event.getGuild(), event.getUser().getIdLong(), "saiu ou foi removido do servidor");
    }

    /**
     * Removes a player from active matches when they are banned from the guild.
     *
     * @param event guild ban event
     */
    @Override
    public void onGuildBan(GuildBanEvent event) {
        gameManager.handlePlayerUnavailable(event.getGuild(), event.getUser().getIdLong(), "foi banido do servidor");
    }
}
