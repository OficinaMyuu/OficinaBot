package ofc.bot.util.content.annotations.listeners;

import java.lang.annotation.*;

/**
 * Classes with this annotation will be registered as event listeners
 * by {@link net.dv8tion.jda.api.JDA#addEventListener(Object...) JDA.addEventListener(Object...)}
 * and must extend the {@link net.dv8tion.jda.api.hooks.ListenerAdapter ListenerAdapter}
 * class, otherwise, an {@link IllegalStateException} is thrown
 * by the {@link ofc.bot.handlers.EntityInitializerManager EntityInitializerManager}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface DiscordEventHandler {}