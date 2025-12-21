package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

@DiscordCommand(name = "server-icon")
public class GuildLogoCommand extends SlashCommand {

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        MessageEmbed embed = embed(guild);

        if (embed == null)
            return Status.GUILD_HAS_NO_ICON;

        return ctx.create()
                .setEmbeds(embed)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Envia a imagem atual do servidor.";
    }

    private MessageEmbed embed(Guild guild) {
        EmbedBuilder builder = new EmbedBuilder();
    
        String name = guild.getName();
        String url = guild.getIconUrl();
        String icon = url == null ? null : url + "?size=1024";
        String desc = guild.getDescription();

        if (url == null)
            return null;

        return builder
                .setTitle(name)
                .setDescription(desc)
                .setImage(icon)
                .setColor(Bot.Colors.DEFAULT)
                .setFooter(name, icon)
                .build();
    }
}