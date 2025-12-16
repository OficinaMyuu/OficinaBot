package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.ImageProxy;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;

@DiscordCommand(name = "avatar")
public class AvatarCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AvatarCommand.class);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        MessageEmbed embed;
        FileUpload upload;
        Member issuer = ctx.getIssuer();
        User user = ctx.getUser();
        Guild guild = ctx.getGuild();
        boolean fromGuild = ctx.getOption("local", false, OptionMapping::getAsBoolean);
        boolean targetProvided = ctx.hasOption("user");

        if (fromGuild) {
            Member targetInput = ctx.getOption("user", OptionMapping::getAsMember);
            Member target = targetProvided ? targetInput : issuer;

            if (target == null)
                return Status.MEMBER_NOT_IN_GUILD;

            ImageProxy avatar = target.getAvatar();
            String avatarId = target.getAvatarId();
            String fileName = String.format("%s.%s", avatarId, isAnimated(avatarId) ? "gif" : "png");

            if (avatar == null)
                return Status.NO_GUILD_AVATAR_PRESENT;

            upload = FileUpload.fromData(downloadAvatar(avatar), fileName);

            embed = embed(upload, guild, target.getUser());
        } else {
            User target = ctx.getOption("user", user, OptionMapping::getAsUser);
            ImageProxy avatar = target.getEffectiveAvatar();
            String avatarId = target.getAvatarId();
            String fileName = String.format("%s.%s", avatarId, isAnimated(avatarId) ? "gif" : "png");

            upload = FileUpload.fromData(downloadAvatar(avatar), fileName);

            embed = embed(upload, guild, target);
        }

        // We do not use a try with resources because JDA's Requester closes
        // the stream automatically once it's sent.
        return ctx.create()
                .setEmbeds(embed)
                .setFiles(upload)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra o avatar de um usuário.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a verificar o avatar."),
                new OptionData(OptionType.BOOLEAN, "local", "Se o avatar mostrado deve ser o específico do servidor atual (Padrão: False).")
        );
    }

    private boolean isAnimated(String url) {
        return url != null && url.startsWith("a_");
    }

    private InputStream downloadAvatar(ImageProxy img) {

        try {
            return img.download(2048).get();
        } catch (InterruptedException | ExecutionException e) {

            LOGGER.warn("Could not download avatar at \"{}\"", img.getUrl(), e);

            // I mean... what else can we do about it?
            return InputStream.nullInputStream();
        }
    }

    private MessageEmbed embed(FileUpload file, Guild guild, User target) {

        EmbedBuilder builder = new EmbedBuilder();
        String name = target.getName();
        String title = "🖼 " + name;
        Color color = Bot.Colors.DISCORD;

        return builder
                .setTitle(title, target.getEffectiveAvatarUrl())
                .setDescription(String.format("Avatar de `%s`", target.getEffectiveName()))
                .setColor(color)
                .setImage("attachment://" + file.getName())
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }
}