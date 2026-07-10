package ofc.bot.domain.entity.enums;

import java.util.List;

public enum StoreItemType {
    GROUP(              "Grupo",                true),
    GROUP_TEXT_CHANNEL( "Chat de Texto",        true),
    GROUP_VOICE_CHANNEL("Chat de Voz",          true),
    UPDATE_GROUP(       "Modificação de Grupo", true),
    ADDITIONAL_BOT(     "Bot Adicional",        true),
    GROUP_SLOT(         "Vaga de Grupo",        true),
    GROUP_PERMISSION(   "Permissão de Grupo",   true),
    PIN_MESSAGE(        "Fixar Mensagem",       true),
    COLOR_ROLE(         "Cargo de Cor",         false),
    COUNTING_RELEASE(   "Liberar Contagem",     false),
    MARRIAGE(           "Casamento",            false);

    private final String name;
    private final boolean isGroup; // For new items in the future

    StoreItemType(String name, boolean isGroup) {
        this.name = name;
        this.isGroup = isGroup;
    }

    public String getName() {
        return this.name;
    }

    public boolean isGroup() {
        return this.isGroup;
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
