package ofc.bot.handlers.interactions.modals.contexts;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.modals.Modal;
import ofc.bot.handlers.interactions.EntityContext;

import java.util.List;
import java.util.UUID;

public class ModalContext extends EntityContext<Modal, ModalContext> {

    private ModalContext(Modal modal) {
        super(modal);
    }

    public static ModalContext of(String customId, String title, List<TextInput> inputs) {
        List<ActionRow> rows = inputs.stream()
                .map(ActionRow::of)
                .toList();

        Modal modal = Modal.create(customId, title)
                .addComponents(rows)
                .build();
        return new ModalContext(modal);
    }

    public static ModalContext of(String customId, String title, TextInput... inputs) {
        return of(customId, title, List.of(inputs));
    }

    public static ModalContext of(String title, List<TextInput> inputs) {
        return of(UUID.randomUUID().toString(), title, inputs);
    }

    public static ModalContext of(String title, TextInput... inputs) {
        return of(title, List.of(inputs));
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
