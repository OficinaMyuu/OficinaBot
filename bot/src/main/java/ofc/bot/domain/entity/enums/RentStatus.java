package ofc.bot.domain.entity.enums;

public enum RentStatus {
    TRIAL(   "Isenção Inicial", "O grupo foi comprado recentemente e não será cobrado no ciclo de aluguel seguinte."),
    FREE(    "Isento",          "O grupo não paga aluguel."),
    PAID(    "Pago",            "O aluguel já foi pago."),
    PENDING( "Pendente",        "O aluguel ainda não foi pago."),
    NOT_PAID("❌ Não Pago",     "O aluguel não foi pago e excedeu o fim do mês.");

    private final String displayStatus;
    private final String description;

    RentStatus(String displayStatus, String description) {
        this.displayStatus = displayStatus;
        this.description = description;
    }

    public String getDisplayStatus() {
        return this.displayStatus;
    }

    public String getDescription() {
        return this.description;
    }
}