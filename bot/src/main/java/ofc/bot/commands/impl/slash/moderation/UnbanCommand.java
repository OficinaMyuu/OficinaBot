package ofc.bot.commands.impl.slash.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
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

@DiscordCommand(name = "unban", permissions = Permission.BAN_MEMBERS)
public class UnbanCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnbanCommand.class);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User user = ctx.getSafeOption("user", OptionMapping::getAsUser);
        String reason = ctx.getOption("reason", OptionMapping::getAsString);
        Guild guild = ctx.getGuild();

        ctx.ack();
        guild.retrieveBan(user).queue(ban -> guild.unban(user).queue(v -> {
            MessageEmbed embed = EmbedFactory.embedPunishment(user, PunishmentType.UNBAN, reason, 0);
            ctx.replyEmbeds(embed);
        }, (err) -> {
            LOGGER.error("Could not unban user {}", user.getId(), err);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        }), (v) -> ctx.reply(Status.USER_IS_NOT_BANNED_FROM_GUILD.args(user.getAsMention())));
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Desbana um usuário do servidor.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a ser desbanido.", true),
                new OptionData(OptionType.STRING, "reason", "O motivo da remoção do banimento.")
                        .setMaxLength(500)
        );
    }
}