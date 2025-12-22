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
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import ofc.bot.util.time.OficinaDuration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "mute", permissions = Permission.MODERATE_MEMBERS)
public class MuteCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MuteCommand.class);
    private static final long MAX_TIMEOUT_SECS = TimeUnit.DAYS.toSeconds(Member.MAX_TIME_OUT_LENGTH);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        String reason = ctx.getOption("reason", OptionMapping::getAsString);
        String fmtDuration = ctx.getSafeOption("duration", OptionMapping::getAsString);
        Guild guild = ctx.getGuild();
        Member self = guild.getSelfMember();

        if (target == null)
            return Status.MEMBER_NOT_FOUND;

        if (!self.canInteract(target))
            return Status.BOT_CANNOT_TIMEOUT_PROVIDED_MEMBER;

        Duration duration;
        try {
            duration = OficinaDuration.ofPattern(fmtDuration).toDuration();
        } catch (IllegalArgumentException e) {
            return Status.INVALID_DURATION_PROVIDED;
        }

        if (duration.getSeconds() > MAX_TIMEOUT_SECS)
            return Status.INVALID_TIMEOUT_DURATION.args(Bot.parsePeriod(MAX_TIMEOUT_SECS), Bot.parseDuration(duration));

        ctx.ack();
        target.timeoutFor(duration).reason(reason).queue(v -> {
            MessageEmbed embed = EmbedFactory.embedPunishment(target.getUser(), PunishmentType.MUTE, reason, duration.getSeconds());
            ctx.replyEmbeds(embed);
        }, (err) -> {
            LOGGER.error("Could not timeout member {}", target.getId(), err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Silencie um membro do servidor.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a ser silenciado", true),
                new OptionData(OptionType.STRING, "duration", "A duração do timeout.", true),
                new OptionData(OptionType.STRING, "reason", "O motivo do timeout.")
                        .setMaxLength(500)
        );
    }
}