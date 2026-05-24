package ofc.bot.domain.entity.enums;

import java.util.List;

public enum StoreItemType {
    GROUP(              "Grupo",                true, 600_000),
    GROUP_TEXT_CHANNEL( "Chat de Texto",        true, 412_500),
    GROUP_VOICE_CHANNEL("Chat de Voz",          true, 300_000),
    UPDATE_GROUP(       "Modificação de Grupo", true, 187_500),
    ADDITIONAL_BOT(     "Bot Adicional",        true, 80_000),
    GROUP_SLOT(         "Vaga de Grupo",        true, 75_000),
    GROUP_PERMISSION(   "Permissão de Grupo",   true, 15_000),
    PIN_MESSAGE(        "Fixar Mensagem",       true, 8_000);

    private final String name;
    private final boolean isGroup; // For new items in the future
    private final int price;

    StoreItemType(String name, boolean isGroup, int price) {
        this.name = name;
        this.isGroup = isGroup;
        this.price = price;
    }

    public String getName() {
        return this.name;
    }

    public boolean isGroup() {
        return this.isGroup;
    }

    public int getPrice() {
        return this.price;
    }

    public static List<StoreItemType> getGroupRefundable() {
        return List.of(
                GROUP,
                GROUP_TEXT_CHANNEL,
                GROUP_VOICE_CHANNEL
        );
    }

    public static StoreItemType fromName(String name) {
        for (StoreItemType type : StoreItemType.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }
}