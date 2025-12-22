package ofc.bot.commands.impl.slash.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.enums.PunishmentType;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "unmute", permissions = Permission.MODERATE_MEMBERS)
public class UnmuteCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnmuteCommand.class);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member member = ctx.getOption("member", OptionMapping::getAsMember);
        String reason = ctx.getOption("reason", OptionMapping::getAsString);
        Guild guild = ctx.getGuild();
        Member self = guild.getSelfMember();

        if (member == null)
            return Status.MEMBER_NOT_FOUND;

        if (!self.canInteract(member))
            return Status.BOT_CANNOT_REMOVE_TIMEOUT_OF_MEMBER;

        if (!member.isTimedOut())
            return Status.MEMBER_IS_NOT_TIMED_OUT;

        ctx.ack();
        member.removeTimeout().reason(reason).queue(v -> {
            MessageEmbed embed = EmbedFactory.embedPunishment(member.getUser(), PunishmentType.UNMUTE, reason, 0);
            ctx.replyEmbeds(embed);
        }, (err) -> {
            LOGGER.error("Could not remove timeout of member {}", member.getId(), err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Dessilencie um usuário do seu servidor.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a ter o timeout removido.", true),
                new OptionData(OptionType.STRING, "reason", "O motivo da remoção do timeout.")
                        .setMaxLength(500)
        );
    }
}