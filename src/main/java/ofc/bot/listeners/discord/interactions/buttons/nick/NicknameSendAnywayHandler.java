package ofc.bot.listeners.discord.interactions.buttons.nick;

import net.dv8tion.jda.api.entities.Member;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.nick.NicknameEmojiPolicy;
import ofc.bot.handlers.nick.NicknameRequestDispatcher;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Misc.CONFIRM_NICKNAME_REQUEST, autoResponseType = AutoResponseType.DEFER_EDIT)
public class NicknameSendAnywayHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NicknameSendAnywayHandler.class);

    private final NicknameRequestDispatcher dispatcher;

    public NicknameSendAnywayHandler(NicknameRequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Member target = ctx.get("target");
        String nickname = ctx.get("nickname");
        NicknameEmojiPolicy.NicknameEmojiReport report = ctx.get("report");

        dispatcher.submit(
                ctx.getGuild(),
                target,
                ctx.getUser(),
                nickname,
                report,
                true,
                request -> ctx.editMessageEmbeds(EmbedFactory.embedNicknameRequestQueued(target, nickname))
                        .setComponents(List.of())
                        .queue(),
                error -> {
                    LOGGER.error("Failed to force queue nickname request for {}", target.getIdLong(), error);
                    ctx.editMessageEmbeds(EmbedFactory.embedNicknameDecisionUnavailable(
                            "Não foi possível enviar o pedido para a fila de aprovação."
                    )).setComponents(List.of()).queue();
                }
        );

        return Status.OK;
    }
}
