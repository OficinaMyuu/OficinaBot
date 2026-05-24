package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.internal.data.BotFiles;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UncheckedIOException;

@DiscordCommand(name = "movie", permissions = Permission.MESSAGE_MANAGE)
public class MovieInstructionsCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovieInstructionsCommand.class);
    private static final File IMAGE_MOBILE = new File(BotFiles.DIR_ASSETS, "movie_instructions_mobile.png");
    private static final File IMAGE_PC = new File(BotFiles.DIR_ASSETS, "movie_instructions_pc.png");

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        MessageChannel channel = ctx.getChannel();

        try {
            FileUpload mobile = FileUpload.fromData(IMAGE_MOBILE);
            FileUpload pc = FileUpload.fromData(IMAGE_PC);

            String text = """
                    Para entrar na transmissão **NO COMPUTADOR** basta entrar na call <#691721622733848596>, passar o mouse em cima do `LIVE`/`AO VIVO` e clicar em `Watch Stream`/`Assistir Transmissão`.
                    
                    No celular é a mesma coisa, basta entrar na call e clicar em `Watch Stream`/`Assistir Transmissão`.
                    
                    ## Abaixo estão duas imagens representativas.
                    """;

            channel.sendMessage(text)
                    .addFiles(pc, mobile)
                    .queue();

            return Status.DONE.setEphm(true);
        } catch (UncheckedIOException e) {
            LOGGER.error("Could not send movie instructions", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Fornece instruções para os usuários sobre como participar do CineMyuu.";
    }
}