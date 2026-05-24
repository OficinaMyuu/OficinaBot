package ofc.bot.util.content.annotations.listeners;

import java.lang.annotation.*;

/**
 * Classes with this annotation will be registered as application-related
 * event listeners.
 * <p>
 * The principle is the same as {@link DiscordEventHandler}, all you have to do is
 * annotate the class and override the methods you are interested in, to be notified
 * about the given event.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApplicationEventHandler {}