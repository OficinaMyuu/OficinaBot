package ofc.bot.util.content.annotations.commands;

import net.dv8tion.jda.api.Permission;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiscordCommand {
    /**
     * The simple command name.
     * <p>
     * See examples for {@code /permissions}, {@code /permissions channel}
     * and {@code /permissions channel view}:
     * <pre>
     *   {@code
     *      @DiscordCommand(name = "permissions") // Correct
     *      @DiscordCommand(name = "permissions channel") // Wrong, should be only "channel"
     *      @DiscordCommand(name = "permissions channel view") // Wrong, should be only "view"
     *   }
     * </pre>
     * <p>
     * Do <b><u>NOT</u></b> use {@code /} (slash), the creation will fail if you do so.
     *
     * @return The simple slash command name.
     */
    String name();

    Permission[] permissions() default {};
}