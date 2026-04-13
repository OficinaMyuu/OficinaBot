package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "avatar")
public class AvatarCommand extends SlashCommand {

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        boolean localAvatarRequested = ctx.getOption("local", false, OptionMapping::getAsBoolean);

        if (localAvatarRequested) {
            return replyWithGuildAvatar(ctx);
        }

        return replyWithGlobalAvatar(ctx);
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

    private InteractionResult replyWithGuildAvatar(SlashCommandContext ctx) {
        Member target = resolveGuildTarget(ctx);

        if (target == null) {
            return Status.MEMBER_NOT_IN_GUILD;
        }

        String avatarUrl = target.getAvatarUrl();
        if (avatarUrl == null) {
            return Status.NO_GUILD_AVATAR_PRESENT;
        }

        MessageEmbed embed = buildAvatarEmbed(ctx.getGuild(), target.getUser(), avatarUrl);
        return ctx.replyEmbeds(embed);
    }

    private InteractionResult replyWithGlobalAvatar(SlashCommandContext ctx) {
        User target = ctx.getOption("user", ctx.getUser(), OptionMapping::getAsUser);
        String avatarUrl = target.getEffectiveAvatarUrl();
        MessageEmbed embed = buildAvatarEmbed(ctx.getGuild(), target, avatarUrl);

        return ctx.replyEmbeds(embed);
    }

    private Member resolveGuildTarget(SlashCommandContext ctx) {
        if (ctx.hasOption("user")) {
            return ctx.getOption("user", OptionMapping::getAsMember);
        }

        return ctx.getIssuer();
    }

    private MessageEmbed buildAvatarEmbed(Guild guild, User target, String avatarUrl) {
        String title = "\uD83D\uDDBC " + target.getName();
        String description = String.format("Avatar de `%s`", target.getEffectiveName());

        return new EmbedBuilder()
                .setTitle(title, avatarUrl + "?size=2048")
                .setDescription(description)
                .setColor(Bot.Colors.DISCORD)
                .setImage(avatarUrl)
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }
}
