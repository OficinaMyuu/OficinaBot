package ofc.bot.handlers.economy.unb;

import com.google.gson.*;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.economy.*;
import ofc.bot.handlers.requests.Route;
import ofc.bot.handlers.requests.requester.impl.UnbelievaBoatRequester;

import java.lang.reflect.Type;

public class UnbelievaBoatClient implements PaymentManager {
    private static final UnbelievaBoatRequester REQUESTER = new UnbelievaBoatRequester();
    private static final UnsupportedOperationException NO_GUILD_EXCEPTION;
    private static final Gson GSON;
    private final String token;

    public UnbelievaBoatClient(String token) {
        Checks.notNull(token, "Token");
        this.token = token;
    }

    public static BankAccount fromJson(long guildId, String json) {
        if (json == null) return null;

        UnbelievaAccount acc = GSON.fromJson(json, UnbelievaAccount.class);
        acc.setGuildId(guildId);
        return acc;
    }

    public BankAccount get(long userId, long guildId) {
        if (guildId == 0)
            throw NO_GUILD_EXCEPTION;

        String json = makeRequest(Route.UnbelievaBoat.GET_BALANCE, null, guildId, userId);
        return fromJson(guildId, json);
    }

    public BankAccount set(long userId, long guildId, long cash, long bank, String reason) {
        if (guildId == 0)
            throw NO_GUILD_EXCEPTION;

        DataObject reqBody = DataObject.empty()
                .put("cash", cash)
                .put("bank", bank)
                .put("reason", reason);

        String json = makeRequest(Route.UnbelievaBoat.SET_BALANCE, reqBody, guildId, userId);
        return fromJson(guildId, json);
    }

    @Override
    public BankAccount update(long userId, long guildId, long cash, long bank, String reason) {
        if (guildId == 0)
            throw NO_GUILD_EXCEPTION;

        DataObject reqBody = DataObject.empty()
                .put("cash", cash)
                .put("bank", bank)
                .put("reason", reason);

        String json = makeRequest(Route.UnbelievaBoat.UPDATE_BALANCE, reqBody, guildId, userId);
        return fromJson(guildId, json);
    }

    @Override
    public CurrencyType getCurrencyType() {
        return CurrencyType.UNBELIEVABOAT;
    }

    @Override
    public BankAction charge(long userId, long guildId, long cash, long bank, String reason) {
        if (guildId == 0)
            throw NO_GUILD_EXCEPTION;

        Checks.notNegative(cash, "Cash");
        Checks.notNegative(bank, "Bank");

        if (cash == 0 && bank == 0) return BankAction.STATIC_SUCCESS_NO_CHANGE;

        BankAccount acc = get(userId, guildId);

        if (!hasEnough(acc, cash, bank)) return BankAction.STATIC_FAILURE_NO_CHANGE;

        BankAccount updatedAcc = update(userId, guildId, -cash, -bank, reason);
        Runnable rollback = () -> {
            // We do not call set(acc) to set the bank account to its initial state
            // fetched earlier, as the user might have updated their balance in the meantime.
            // Also, we are safe to directly provide 'cash' and 'bank' from the parameters
            // as they will never be negative
            update(userId, guildId, cash, bank, "Refund of earlier request (" + reason + ")");
        };

        if (isInDebt(updatedAcc)) {
            rollback.run();
            return BankAction.STATIC_FAILURE_NO_CHANGE;
        }

        return new BankAction(true, true, rollback);
    }

    private boolean isInDebt(BankAccount acc) {
        return acc == null
                || acc.getCash() < 0
                || acc.getBank() < 0;
    }

    private boolean hasEnough(BankAccount acc, long cash, long bank) {
        return acc != null
                && acc.getCash() >= cash
                && acc.getBank() >= bank;
    }

    private String makeRequest(Route route, DataObject body, Object... path) {
        return route.create(path)
                .addHeader("Authorization", token)
                .setBody(body)
                .send(REQUESTER, (map, code) -> map.isOk() ? map.asString() : null);
    }

    private static class LongInfinityDeserializer implements JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String value = json.getAsString();

            if ("Infinity".equals(value))
                return Long.MAX_VALUE;

            double doubleValue = json.getAsDouble();

            if (doubleValue > Long.MAX_VALUE)
                return Long.MAX_VALUE;

            return (long) doubleValue;
        }
    }

    static {
        NO_GUILD_EXCEPTION = new UnsupportedOperationException(
                "UnbelievaBoat is a guild-based economy, all requests require a valid \"guild_id\" to exist"
        );

        GSON = new GsonBuilder()
                .registerTypeAdapter(long.class, new LongInfinityDeserializer())
                .create();
    }
}