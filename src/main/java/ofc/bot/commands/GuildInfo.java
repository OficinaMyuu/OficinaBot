package ofc.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import ofc.bot.content.annotations.commands.DiscordCommand;
import ofc.bot.handlers.commands.contexts.CommandContext;
import ofc.bot.handlers.commands.responses.results.CommandResult;
import ofc.bot.handlers.commands.responses.results.Status;
import ofc.bot.handlers.commands.slash.SlashCommand;

import java.awt.*;
import java.util.List;

@DiscordCommand(name = "serverinfo", description = "Informações gerais sobre o servidor.")
public class GuildInfo extends SlashCommand {

    @Override
    public CommandResult onCommand(CommandContext ctx) {

        Guild guild = ctx.getGuild();
        MessageEmbed embed = embed(guild);

        ctx.replyEmbeds(embed);

        return Status.PASSED;
    }

    private MessageEmbed embed(Guild guild) {

        EmbedBuilder builder = new EmbedBuilder();

        long timeCreated = guild.getTimeCreated().toEpochSecond();
        String creation = String.format("<t:%d>\n<t:%1$d:R>", timeCreated);
        Member owner = guild.retrieveOwner().complete();
        String ownerName = owner == null ? "Not found" : owner.getEffectiveName();
        String banner = guild.getBannerUrl() == null ? "" : guild.getBannerUrl() + "?size=2048";

        List<GuildChannel> channels = guild.getChannels(true);
        List<TextChannel> textChannels = guild.getTextChannels();
        List<VoiceChannel> audioChannels = guild.getVoiceChannels();
        List<Category> categories = guild.getCategories();
        List<ForumChannel> forums = guild.getForumChannels();
        List<ThreadChannel> threads = guild.getThreadChannels();

        builder
                .setTitle("<a:M_Myuu:643942157325041668> " + guild.getName())
                .setThumbnail(guild.getIconUrl())
                .setColor(new Color(193, 126, 142))
                .addField("🌐 Server ID", "`" + guild.getOwnerIdLong() + "`", true)
                .addField("📅 Criação", creation, true)
                .addField("👑 Dono", "`" + ownerName + "`", true)
                .addField("💬 Canais (e Categorias) (" + channels.size() + ")", String.format("""
                        🔉 Áudio: `%d`
                        ⚽ Categorias: `%d`
                        💭 Fóruns: `%s`
                        📝 Texto: `%d`
                        🎈 Threads: `%d`
                        """,
                        audioChannels.size(),
                        categories.size(),
                        forums.size(),
                        textChannels.size(),
                        threads.size()
                ), true)
                .setImage(banner)
                .setFooter(guild.getName(), guild.getIconUrl());

        builder.addField("👥 Membros (" + guild.getMemberCount() + ")", "", false);

        return builder.build();
    }
}