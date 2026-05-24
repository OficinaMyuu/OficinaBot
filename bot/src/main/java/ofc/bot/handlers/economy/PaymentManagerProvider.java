package ofc.bot.handlers.economy;

import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.economy.oficina.OficinaBankClient;
import ofc.bot.handlers.economy.unb.UnbelievaBoatClient;
import ofc.bot.util.Bot;

public final class PaymentManagerProvider {
    private static UnbelievaBoatClient ub;
    private static OficinaBankClient ofc;

    private PaymentManagerProvider() {}

    public static UnbelievaBoatClient getUnbelievaBoatClient() {
        if (ub == null) {
            String token = Bot.get("unbelievaboat.token");
            ub = new UnbelievaBoatClient(token);
        }
        return ub;
    }

    public static OficinaBankClient getOficinaBankClient() {
        if (ofc == null) {
            UserEconomyRepository userEco = Repositories.getUserEconomyRepository();
            ofc = new OficinaBankClient(userEco);
        }
        return ofc;
    }

    public static PaymentManager fromType(CurrencyType type) {
        return switch (type) {
            case OFICINA -> getOficinaBankClient();
            case UNBELIEVABOAT -> getUnbelievaBoatClient();
        };
    }
}