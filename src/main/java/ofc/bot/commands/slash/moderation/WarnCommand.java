package ofc.bot.commands.slash.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.sqlite.repository.AutomodActionRepository;
import ofc.bot.domain.sqlite.repository.MemberPunishmentRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.moderation.PunishmentData;
import ofc.bot.handlers.moderation.PunishmentManager;
import ofc.bot.handlers.moderation.Reason;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "warn", permissions = Permission.BAN_MEMBERS)
public class WarnCommand extends SlashCommand {
    private final PunishmentManager punishmentManager;

    public WarnCommand(MemberPunishmentRepository pnshRepo, AutomodActionRepository modActRepo) {
        this.punishmentManager = new PunishmentManager(pnshRepo, modActRepo);
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String reason = ctx.getSafeOption("reason", OptionMapping::getAsString);
        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        Member issuer = ctx.getIssuer();
        MessageChannel chan = ctx.getChannel();

        if (target == null)
            return Status.MEMBER_NOT_FOUND;

        if (target.equals(issuer))
            return Status.YOU_CANNOT_PUNISH_YOURSELF;

        if (target.hasPermission(Permission.ADMINISTRATOR))
            return Status.TARGET_IS_IMMUNE_TO_PUNISHMENTS;

        PunishmentData warnData = new PunishmentData(chan, target, issuer, Reason.of(reason));
        MessageEmbed punishEmbed = punishmentManager.createPunishment(warnData);
        return ctx.replyEmbeds(punishEmbed);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Puna um membro por alguma conduta indevida.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a ser advertido.", true),
                new OptionData(OptionType.STRING, "reason", "O motivo da punição.", true)
                        .setRequiredLength(4, 2000)
        );
    }
}