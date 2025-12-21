package ofc.bot.commands.api;

/**
 * The functional logic of a command.
 * <p>
 * Every slash/legacy/message abstract class must implement this.
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * Executes the command logic.
     *
     * @param ctx The context containing args, user info, and reply methods.
     */
    void onCommand(ICommandContext<?> ctx);
}