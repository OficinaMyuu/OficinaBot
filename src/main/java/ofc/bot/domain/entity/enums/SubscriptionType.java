package ofc.bot.domain.entity.enums;

public enum SubscriptionType {
    USERINFO_ICON("Ícone de Userinfo"),
    GROUP_RENT("Aluguél de Grupo");

    private final String name;

    SubscriptionType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}