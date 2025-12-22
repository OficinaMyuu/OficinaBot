package ofc.bot.commands.impl.slash.additionals;

import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.modals.Modal;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "additionals roles")
public class AdditionalRolesCommand extends SlashSubcommand {

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long chanId = ctx.getSafeOption("channel", OptionMapping::getAsChannel).getIdLong();
        Message.Attachment img = ctx.getOption("attachment", OptionMapping::getAsAttachment);
        String hexColor = ctx.getOption("color", OptionMapping::getAsString);
        int maxChoices = ctx.getOption("max-choices", -1, OptionMapping::getAsInt);

        int color = 0;
        try {
            color = hexColor == null ? color : Integer.parseInt(hexColor, 16);
        } catch (NumberFormatException e) {
            return Status.INVALID_COLOR_PROVIDED;
        }

        Modal modal = EntityContextFactory.createChoosableRolesModal(img, chanId, color, maxChoices);
        return ctx.replyModal(modal);
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Configura uma mensagem para membros ganharem cargos adicionais.";
    }

    @Override
    @NotNull
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.CHANNEL, "channel", "O canal a ser enviado a mensagem do menu.", true)
                        .setChannelTypes(ChannelType.TEXT),

                new OptionData(OptionType.ATTACHMENT, "attachment", "A imagem/banner da embed."),
                new OptionData(OptionType.STRING, "color", "A cor da embed.")
                        .setRequiredLength(6, 6),

                new OptionData(OptionType.INTEGER, "max-choices", "Quantas opções o usuário pode, no máximo, escolher.")
                        .setRequiredRange(1, SelectMenu.OPTIONS_MAX_AMOUNT)
        );
    }
}