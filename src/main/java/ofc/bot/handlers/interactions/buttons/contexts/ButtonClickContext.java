package ofc.bot.handlers.interactions.buttons.contexts;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.replacer.ComponentReplacer;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import ofc.bot.handlers.interactions.InteractionSubmitContext;

public class ButtonClickContext extends InteractionSubmitContext<ButtonContext, ButtonInteraction> {

    public ButtonClickContext(ButtonContext context, ButtonInteraction itr) {
        super(context, itr);
    }

    @Override
    public void ack(boolean ephemeral) {
        if (!isAcknowledged())
            getSource().deferReply(ephemeral).queue();
    }

    public void ackEdit() {
        if (!isAcknowledged())
            getSource().deferEdit().queue();
    }

    /**
     * Disables the button the user has clicked.
     */
    public void disable() {
        ButtonInteraction source = getSource();
        Button clicked = source.getButton();
        Message msg = getMessage();
        MessageComponentTree components = msg.getComponentTree();

        ComponentReplacer replacer = ComponentReplacer.byUniqueId(clicked.getUniqueId(),
                clicked.asDisabled()
        );

        MessageComponentTree updated = components.replace(replacer);
        msg.editMessageComponents(updated).queue();
    }

    public void disableAll() {
        ButtonInteraction source = getSource();
        Message msg = getMessage();
        MessageComponentTree components = msg.getComponentTree();
        ComponentReplacer replacer = ComponentReplacer.of(Button.class, (b) -> true, Button::asDisabled);

        MessageComponentTree updated = components.replace(replacer);
        msg.editMessageComponents(updated).queue();
    }

    public MessageEditAction editMessage(String content) {
        return getMessage().editMessage(content);
    }

    public final MessageEditAction editMessageEmbeds(MessageEmbed... embeds) {
        return getMessage().editMessageEmbeds(embeds);
    }

    public MessageEditAction editMessageComponents(MessageTopLevelComponent... components) {
        return getMessage().editMessageComponents(components);
    }

    public Message getMessage() {
        return getSource().getMessage();
    }
}