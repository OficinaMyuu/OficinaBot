package ofc.bot.util.content;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.Main;
import net.dv8tion.jda.api.entities.channel.Channel;
import ofc.bot.util.Bot;

public enum Channels {
    GENERAL("channels.oficina_erik.general"),
    MOV_CALL_LOG("channels.oficina_erik.log_mov_call"),
    TIMEOUT_LOG("channels.oficina_erik.timeout_log"),
    AMONG_US_ROLE("channels.oficina_erik.among_us_role"),
    GUILD_STAFF("channels.oficina_erik.guild_staff"),
    BDAY_REMINDER("channels.oficina_erik.birthday_reminder"),
    LATE_GROUPS_INVOICE("channels.oficina_erik.late_groups_invoice"),
    REGISTRY("channels.oficina_erik.registry"),
    SOLO_GATEWAY("channels.oficina_erik.solo_gateway"),
    CHANGELOG("channels.oficina_erik.changelog"),
    LEVEL_UP("channels.oficina_erik.level_up"),
    AUTOMOD_LOG("channels.oficina_erik.automod_log"),

    /* ---------- Categories ---------- */
    TICKETS("channels.oficina_erik.tickets");

    private final String key;

    Channels(String key) {
        this.key = key;
    }

    public String fetchId() {
        return Bot.get(this.key);
    }

    public long fetchIdLong() {
        String val = fetchId();
        return val == null ? 0 : Long.parseLong(val);
    }

    public boolean isSame(String chanId) {
        String id = fetchId();
        return id != null && id.equals(chanId);
    }

    public boolean isSame(long chanId) {
        return isSame(Long.toString(chanId));
    }

    public String getKey() {
        return this.key;
    }

    public <T extends Channel> T channel(Class<T> type) {
        String id = Bot.getSafe(this.key);
        return Main.getApi().getChannelById(type, id);
    }

    public TextChannel textChannel() {
        return channel(TextChannel.class);
    }
}