package ofc.bot.listeners.discord.guilds.voice;

import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.handlers.giveaway.GiveawayService;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class GiveawayVoiceConditionListener extends ListenerAdapter {
    private final GiveawayService giveawayService;

    public GiveawayVoiceConditionListener(GiveawayService giveawayService) {
        this.giveawayService = giveawayService;
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannelUnion left = event.getChannelLeft();
        AudioChannelUnion joined = event.getChannelJoined();
        if (left == null) {
            return;
        }

        if (joined != null && joined.getIdLong() == left.getIdLong()) {
            return;
        }

        giveawayService.removeVoiceLockedEntries(event.getMember().getIdLong(), left.getIdLong());
    }
}
