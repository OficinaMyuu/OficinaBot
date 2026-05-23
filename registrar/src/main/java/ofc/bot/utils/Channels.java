package ofc.bot.utils;

import net.dv8tion.jda.api.entities.channel.Channel;
import ofc.bot.RegisterMaster;
import ofc.bot.internal.BotData;

public enum Channels {
    REGISTER("channels.registry"),
    RL("channels.registry.log");

    private final String key;

    Channels(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public long fetchId() {
        return BotData.getSafe(this.key, Long::parseLong);
    }

    public <T extends Channel> T channel(Class<T> type) {
        long id = BotData.getSafe(this.key, Long::parseLong);
        return RegisterMaster.getApi().getChannelById(type, id);
    }
}