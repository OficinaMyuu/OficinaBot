package ofc.bot.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.utils.Channels;

public class DeleteFormerMemberMessages extends ListenerAdapter {

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent e) {
        TextChannel channel = Channels.REGISTER.channel(TextChannel.class);
        long userId = e.getUser().getIdLong();

        channel.getHistory().retrievePast(50).queue(msgs -> {
            for (Message msg : msgs) {
                if (msg.getAuthor().getIdLong() == userId) {
                    msg.delete().queue();
                }
            }
        });
    }
}