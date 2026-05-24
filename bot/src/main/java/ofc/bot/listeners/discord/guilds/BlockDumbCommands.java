package ofc.bot.listeners.discord.guilds;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordEventHandler
public class BlockDumbCommands extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = message.getAuthor();
        MessageChannelUnion channel = message.getChannel();
        List<MessageEmbed> embeds = message.getEmbeds();

        if (author.getIdLong() != 297153970613387264L || embeds.isEmpty()) return;

        for (MessageEmbed embed : embeds) {
            if (shouldDelete(embed)) {
                Bot.delete(message);
                channel.sendMessage("Este comando é recomendável que use o bot da Oficina.\nEx: Use `/userinfo` ou `/avatar` ao invés disso.").queue();
                break;
            }
        }
    }

    private boolean shouldDelete(MessageEmbed embed) {
        MessageEmbed.AuthorInfo author = embed.getAuthor();
        String title = embed.getTitle();
        String authorName = author == null ? null : author.getName();

        List<MessageEmbed.Field> fields = embed.getFields();

        // Checking for "avatar"
        if (title != null && title.startsWith("\uD83D\uDDBC"))
            return true;

        for (MessageEmbed.Field field : fields) {
            String name = field.getName();

            if (name != null && name.startsWith("Exibe"))
                return true;

            if (authorName != null && authorName.startsWith("Informações"))
                return true;

            if (name != null && name.stripTrailing().toUpperCase().endsWith("ID"))
                return true;
        }

        return false;
    }
}