package ofc.bot.commands.slash.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.MemberPunishment;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "infractions", permissions = Permission.MANAGE_SERVER)
public class InfractionsCommand extends SlashCommand {
    public static final int PAGE_SIZE = 1;

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        boolean showInactive = ctx.getOption("show-inactive", false, OptionMapping::getAsBoolean);
        long targetId = target.getIdLong();
        long guildId = guild.getIdLong();
        PageItem<MemberPunishment> infrs = Paginator.viewInfractions(
                targetId, guildId, PAGE_SIZE, 0, showInactive);

        if (infrs.isEmpty())
            return Status.USER_HAS_NO_INFRACTIONS;

        MemberPunishment infr = infrs.get(0);
        boolean active = infr.isActive();
        List<Button> btns = EntityContextFactory.createInfractionsButtons(
                infr.getId(), active, targetId, infrs.getPageIndex(), showInactive, infrs.hasMore());
        MessageEmbed embed = EmbedFactory.embedInfractions(target, guild, infrs, infr.getModeratorId());

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(btns)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Veja as infrações de um usuário.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário que deseja ver as infrações.", true),
                new OptionData(OptionType.BOOLEAN, "show-inactive", "Mostrar até infrações já apagadas? (Padrão: False).")
        );
    }
}