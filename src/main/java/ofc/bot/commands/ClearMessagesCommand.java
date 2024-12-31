package ofc.bot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@DiscordCommand(
        name = "clear",
        description = "Limpa de 2 a 100 mensagens do chat de uma só vez.",
        cooldown = 1,
        permission = Permission.MESSAGE_MANAGE
)
public class ClearMessagesCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearMessagesCommand.class);

    @Override
    public InteractionResult onSlashCommand(SlashCommandContext ctx) {
        ctx.ack(true);

        GuildMessageChannel current = (GuildMessageChannel) ctx.getChannel();
        GuildMessageChannel channel = (GuildMessageChannel) ctx.getOption("channel", current, OptionMapping::getAsChannel);
        int amount = ctx.getSafeOption("amount", OptionMapping::getAsInt);

        channel.getHistory().retrievePast(amount).queue(msgs -> {
            int msgCount = msgs.size();

            channel.deleteMessages(msgs).queue(s -> {
                ctx.reply(Status.MESSAGES_SUCCESSFULLY_DELETED.args(msgCount, channel.getName()));
            }, err -> {
                LOGGER.error("Could not clear {} messages from #{}", msgCount, channel.getId(), err);
                ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
            });
        });
        return Status.OK;
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "amount", "A quantidade de mensagens a ser limpada.", true)
                        .setRequiredRange(2, 100),

                new OptionData(OptionType.CHANNEL, "channel", "O canal ser limpado as mensagens (Padrão: atual).")
                        .setChannelTypes(getTextChannelTypes())
        );
    }

    private static ChannelType[] getTextChannelTypes() {
        return Arrays.stream(ChannelType.values())
                .filter(ChannelType::isMessage)
                .filter(ChannelType::isGuild)
                .toArray(ChannelType[]::new);
    }
}