package ofc.bot.handlers.interactions.modals.contexts;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.modals.Modal;
import ofc.bot.handlers.interactions.EntityContext;

import java.util.List;
import java.util.UUID;

public class ModalContext extends EntityContext<Modal, ModalContext> {

    private ModalContext(Modal modal) {
        super(modal);
    }

    public static ModalContext of(String customId, String title, List<Label> labels) {
        Modal modal = Modal.create(customId, title)
                .addComponents(labels)
                .build();
        return new ModalContext(modal);
    }

    public static ModalContext of(String customId, String title, Label... labels) {
        return of(customId, title, List.of(labels));
    }

    public static ModalContext of(String title, List<Label> labels) {
        return of(UUID.randomUUID().toString(), title, labels);
    }

    public static ModalContext of(String title, Label... labels) {
        return of(title, List.of(labels));
    }

    @Override
    public ModalContext setDisabled(boolean disabled) {
        return this;
    }

    @Override
    public String getId() {
        return getEntity().getId();
    }
}
