package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.entities.Guild;

public interface AccumulatorDiscordBridge {
    boolean memberExists(Guild guild, long userId);

    boolean roleExists(Guild guild, long roleId);

    boolean memberHasRole(Guild guild, long userId, long roleId);

    void addRole(Guild guild, long userId, long roleId);

    void removeRole(Guild guild, long userId, long roleId);
}
