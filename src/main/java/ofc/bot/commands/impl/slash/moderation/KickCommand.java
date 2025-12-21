package ofc.bot.commands.impl.slash.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
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

@DiscordCommand(name = "kick", permissions = Permission.KICK_MEMBERS)
public class KickCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(KickCommand.class);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User issuer = ctx.getUser();
        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        String reason = ctx.getSafeOption("reason", OptionMapping::getAsString);
        Guild guild = ctx.getGuild();
        Member self = guild.getSelfMember();
        long issuerId = issuer.getIdLong();
        long guildId = guild.getIdLong();

        if (target == null)
            return Status.MEMBER_NOT_FOUND;

        if (target.getIdLong() == issuerId)
            return Status.YOU_CANNOT_KICK_YOURSELF;

        if (!self.canInteract(target))
            return Status.BOT_CANNOT_KICK_PROVIDED_MEMBER;

        target.kick().reason(reason).queue(v -> {
            MessageEmbed embed = EmbedFactory.embedPunishment(target.getUser(), PunishmentType.KICK, reason, 0);

            ctx.replyEmbeds(embed);
        }, (err) -> {
            LOGGER.error("Could not kick member {} from guild {}", target.getId(), guildId, err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Expulsa um membro do servidor.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a ser expulso", true),
                new OptionData(OptionType.STRING, "reason", "O motivo da expulsão.", true)
                        .setRequiredLength(4, 500)
        );
    }
}
