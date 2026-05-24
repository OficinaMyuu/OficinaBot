package ofc.bot.util.content.annotations.listeners;

import ofc.bot.handlers.EntityInitializerManager;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes with this annotation will be registered as listeners
 * only for {@link net.dv8tion.jda.api.interactions.components.buttons.Button Button} clicks
 * and must implement the {@link InteractionListener BotButtonListener}
 * interface, otherwise, an {@link IllegalStateException} is thrown
 * by the {@link EntityInitializerManager EntityHandlersInitializers}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InteractionHandler {
    String scope();
    AutoResponseType autoResponseType() default AutoResponseType.NONE;
}