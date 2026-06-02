package ofc.bot.handlers.economy.unb;

import com.google.gson.*;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.economy.*;
import ofc.bot.handlers.requests.Route;
import ofc.bot.handlers.requests.requester.impl.UnbelievaBoatRequester;

import java.lang.reflect.Type;

public class UnbelievaBoatClient implements PaymentManager {
    private static final UnsupportedOperationException NO_GUILD_EXCEPTION;
    private static final Gson GSON;
    private final String token;
    private final UnbelievaBoatRequester requester;

    /**
     * Creates an UnbelievaBoat economy client using the provided API token.
     * <p>
     * UnbelievaBoat expects the raw token in the {@code Authorization} header,
     * without a bearer prefix.
     *
     * @param token the UnbelievaBoat API token.
     * @throws IllegalArgumentException if {@code token} is blank.
     */
    public UnbelievaBoatClient(String token) {
        this(token, new UnbelievaBoatRequester());
    }

    UnbelievaBoatClient(String token, UnbelievaBoatRequester requester) {
        Checks.notEmpty(token, "Token");
        if (token.isBlank())
            throw new IllegalArgumentException("Token may not be blank");

        Checks.notNull(requester, "Requester");
        this.token = token;
        this.requester = requester;
    }

    /**
     * Parses an UnbelievaBoat balance response into this bot's bank account abstraction.
     *
     * @param guildId the guild the response belongs to.
     * @param json the UnbelievaBoat JSON response body, or {@code null}.
     * @return a parsed bank account, or {@code null} when {@code json} is {@code null}.
     */
    public static BankAccount fromJson(long guildId, String json) {
        if (json == null) return null;

        UnbelievaAccount acc = GSON.fromJson(json, UnbelievaAccount.class);
        acc.setGuildId(guildId);
        return acc;
    }

    /**
     * Fetches a user's UnbelievaBoat balance in a guild.
     *
     * @param userId the Discord user id.
     * @param guildId the Discord guild id.
     * @return the current bank account, or {@code null} if UnbelievaBoat did not return a successful response.
     * @throws UnsupportedOperationException if {@code guildId} is zero.
     */
    public BankAccount get(long userId, long guildId) {
        validateGuildId(guildId);

        String json = makeRequest(Route.UnbelievaBoat.GET_BALANCE, null, guildId, userId);
        return fromJson(guildId, json);
    }

    /**
     * Replaces a user's UnbelievaBoat cash and bank balances in a guild.
     *
     * @param userId the Discord user id.
     * @param guildId the Discord guild id.
     * @param cash the new cash balance.
     * @param bank the new bank balance.
     * @param reason the optional audit reason sent to UnbelievaBoat.
     * @return the updated bank account, or {@code null} if UnbelievaBoat did not return a successful response.
     * @throws UnsupportedOperationException if {@code guildId} is zero.
     */
    public BankAccount set(long userId, long guildId, long cash, long bank, String reason) {
        validateGuildId(guildId);

        DataObject reqBody = createBalanceBody(cash, bank, reason);
        String json = makeRequest(Route.UnbelievaBoat.SET_BALANCE, reqBody, guildId, userId);
        return fromJson(guildId, json);
    }

    /**
     * Adds to, or subtracts from, a user's UnbelievaBoat cash and bank balances in a guild.
     *
     * @param userId the Discord user id.
     * @param guildId the Discord guild id.
     * @param cash the cash delta.
     * @param bank the bank delta.
     * @param reason the optional audit reason sent to UnbelievaBoat.
     * @return the updated bank account, or {@code null} if UnbelievaBoat did not return a successful response.
     * @throws UnsupportedOperationException if {@code guildId} is zero.
     */
    @Override
    public BankAccount update(long userId, long guildId, long cash, long bank, String reason) {
        validateGuildId(guildId);

        DataObject reqBody = createBalanceBody(cash, bank, reason);
        String json = makeRequest(Route.UnbelievaBoat.UPDATE_BALANCE, reqBody, guildId, userId);
        return fromJson(guildId, json);
    }

    /**
     * Returns the currency type handled by this client.
     *
     * @return {@link CurrencyType#UNBELIEVABOAT}.
     */
    @Override
    public CurrencyType getCurrencyType() {
        return CurrencyType.UNBELIEVABOAT;
    }

    /**
     * Attempts to charge a user from their UnbelievaBoat cash and bank balances.
     * <p>
     * The method first verifies the current balance, applies a negative update,
     * and rolls the update back if UnbelievaBoat returns a negative balance.
     *
     * @param userId the Discord user id.
     * @param guildId the Discord guild id.
     * @param cash the cash amount to remove.
     * @param bank the bank amount to remove.
     * @param reason the optional audit reason sent to UnbelievaBoat.
     * @return the charge result and rollback action.
     * @throws IllegalArgumentException if {@code cash} or {@code bank} are negative.
     * @throws UnsupportedOperationException if {@code guildId} is zero.
     */
    @Override
    public BankAction charge(long userId, long guildId, long cash, long bank, String reason) {
        validateGuildId(guildId);

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

    private void validateGuildId(long guildId) {
        if (guildId == 0)
            throw NO_GUILD_EXCEPTION;
    }

    private DataObject createBalanceBody(long cash, long bank, String reason) {
        return DataObject.empty()
                .put("cash", cash)
                .put("bank", bank)
                .put("reason", reason);
    }

    private String makeRequest(Route route, DataObject body, Object... path) {
        return route.create(path)
                .addHeader("Authorization", token)
                .setBody(body)
                .send(this.requester, (map, code) -> map.isOk() ? map.asString() : null);
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
