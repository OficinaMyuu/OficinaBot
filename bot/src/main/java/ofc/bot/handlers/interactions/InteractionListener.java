package ofc.bot.handlers.interactions;

import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;

public interface InteractionListener<T extends InteractionSubmitContext<?, ?>> {

    /**
     * Executes the primary logic for this interaction.
     *
     * @param ctx The context of the interaction.
     * @return An {@link InteractionResult} representing the "exit status" of the execution.
     */
    InteractionResult onExecute(T ctx);

    /**
     * Validates whether the interaction should proceed.
     * <p>
     * This method acts as a preliminary check before executing the main logic in
     * {@link #onExecute(InteractionSubmitContext)}. It allows for early termination of the process
     * if certain conditions are not met.
     * <p>
     * If this method returns {@code false}, {@link #onExecute(InteractionSubmitContext)} will not be called.
     * The validation logic is separate from the core execution logic to allow
     * for better separation of concerns and reusability.
     *
     * @param ctx The interaction context, which is also passed to {@link #onExecute(InteractionSubmitContext)}.
     * @return {@code true} if the execution should proceed, {@code false} to prevent execution.
     */
    default boolean validate(T ctx) {
        return true;
    }

    default String getScope() {
        InteractionHandler ann = getAnn();
        return ann.scope();
    }

    default AutoResponseType getAutoResponseType() {
        InteractionHandler ann = getAnn();
        return ann.autoResponseType();
    }

    private InteractionHandler getAnn() {
        var clazz = this.getClass();
        InteractionHandler ann = clazz.getDeclaredAnnotation(InteractionHandler.class);

        if (ann == null)
            throw new IllegalStateException(
                    "Composed interaction listener " + clazz.getName() + " is not annotated with @" +
                    clazz.getName());

        return ann;
    }
}