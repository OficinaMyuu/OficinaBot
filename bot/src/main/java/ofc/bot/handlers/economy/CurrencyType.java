package ofc.bot.handlers.economy;

import net.dv8tion.jda.api.entities.emoji.Emoji;

public enum CurrencyType {
    OFICINA(      "Oficina",       "<:oficina:1304262179935092778>"),
    UNBELIEVABOAT("UnbelievaBoat", "<:unbelievaboat:1304262330950881300>");

    private final String name;
    private final String emojiFormat;

    CurrencyType(String name, String emojiFormat) {
        this.name = name;
        this.emojiFormat = emojiFormat;
    }

    public String getName() {
        return this.name;
    }

    public Emoji getEmoji() {
        return Emoji.fromFormatted(this.emojiFormat);
    }

    public String getFormatted() {
        return String.format("%s %s", this.emojiFormat, this.name);
    }

    public static CurrencyType fromName(String name) {
        for (CurrencyType c : CurrencyType.values()) {
            if (c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }
}