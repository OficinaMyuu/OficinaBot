package ofc.bot.handlers.interactions.buttons;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.sqlite.repository.AppUserBanRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.InteractionMemoryManager;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DiscordEventHandler
public class ButtonInteractionGateway extends ListenerAdapter {
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final InteractionMemoryManager mngr = InteractionMemoryManager.getManager();
    private final AppUserBanRepository appBanRepo;

    public ButtonInteractionGateway(AppUserBanRepository appBanRepo) {
        this.appBanRepo = appBanRepo;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        Member member = e.getMember();
        String buttonId = e.getComponentId();
        ButtonContext buttonCtx = mngr.get(buttonId);

        if (!e.isFromGuild() || member == null || buttonCtx == null) return;

        String scope = buttonCtx.getScope();
        List<InteractionListener<ButtonClickContext>> listeners = mngr.getListeners(scope);

        if (listeners.isEmpty()) return;

        if (!buttonCtx.isPermitted(member)) {
            e.reply("Você não pode clicar neste botão.").setEphemeral(true).queue();
            return;
        }

        ButtonClickContext clickContext = new ButtonClickContext(buttonCtx, e);
        // Is the user banned from our application?
        long userId = clickContext.getUserId();
        if (appBanRepo.isBanned(userId)) {
            clickContext.reply(Status.YOU_ARE_BANNED_FROM_THIS_BOT);
            return;
        }

        listeners.forEach(ls -> handleAutoResponse(ls.getAutoResponseType(), clickContext));
        mngr.remove(buttonId);

        for (var l : listeners) {
            EXECUTOR.execute(() -> {
                if (!l.validate(clickContext)) return;

                InteractionResult state = l.onExecute(clickContext);
                if (state.getContent() != null)
                    clickContext.reply(state);
            });
        }
    }

    private void handleAutoResponse(AutoResponseType type, ButtonClickContext ctx) {
        switch (type) {
            case THINKING, THINKING_EPHEMERAL -> ctx.ack(type.isEphemeral());
            case DEFER_EDIT -> ctx.ackEdit();
        }
    }
}