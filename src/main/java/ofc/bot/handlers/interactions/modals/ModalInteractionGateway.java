package ofc.bot.handlers.interactions.modals;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.InteractionMemoryManager;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.modals.contexts.ModalContext;
import ofc.bot.handlers.interactions.modals.contexts.ModalSubmitContext;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

@DiscordEventHandler
public class ModalInteractionGateway extends ListenerAdapter {
    private static final ExecutorService EXECUTOR = ForkJoinPool.commonPool();
    private final InteractionMemoryManager mngr = InteractionMemoryManager.getManager();

    @Override
    public void onModalInteraction(ModalInteractionEvent e) {
        Member member = e.getMember();
        String modalId = e.getModalId();
        ModalContext modalCtx = mngr.get(modalId);

        if (!e.isFromGuild() || member == null) return;

        if (modalCtx == null) return;

        String scope = modalCtx.getScope();
        List<InteractionListener<ModalSubmitContext>> listeners = mngr.getListeners(scope);

        if (listeners.isEmpty()) return;

        // There is no need to check if the user is permitted to submit
        // the Modal as no one else can even see it.
        ModalSubmitContext submitContext = new ModalSubmitContext(modalCtx, e);
        listeners.forEach(ls -> handleAutoResponse(ls.getAutoResponseType(), submitContext));

        mngr.remove(modalId);

        for (var l : listeners) {
            EXECUTOR.execute(() -> {
                if (!l.validate(submitContext)) return;
                InteractionResult state = l.onExecute(submitContext);

                if (state.getContent() != null)
                    submitContext.reply(state);
            });
        }
    }

    private void handleAutoResponse(AutoResponseType type, ModalSubmitContext ctx) {
        switch (type) {
            case THINKING, THINKING_EPHEMERAL -> ctx.ack(type.isEphemeral());
            case DEFER_EDIT -> throw new UnsupportedOperationException(
                    "Cannot send a defer edit response to Modal interactions");
        }
    }
}