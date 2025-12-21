package ofc.bot.commands.api;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ICommandContext<S> {

    @NotNull
    User getUser();

    @NotNull
    Member getMember();

    @NotNull
    Guild getGuild();

    GuildMessageChannelUnion getChannel();

    @NotNull
    S getSource();

    void defer(boolean ephemeral);

    default void defer() {
        defer(false);
    }

    void reply(@NotNull String key, @NotNull DiscordLocale locale, @NotNull Map<String, Object> args);

    default void reply(@NotNull String key, @NotNull DiscordLocale locale) {
        reply(key, locale, Map.of());
    }

    default void reply(@NotNull String key) {
        reply(key, Bot.DEFAULT_DISCORD_LOCALE);
    }

    void replyRaw(@NotNull String message);

    IReplyAction replyRich();
}