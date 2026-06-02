package ofc.bot.handlers.economy.unb;

import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.requests.RequestMapper;
import ofc.bot.handlers.requests.exceptions.HttpRequestException;
import ofc.bot.handlers.requests.requester.impl.UnbelievaBoatRequester;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class UnbelievaBoatClientTest {
    @Test
    void sendsRawAuthorizationTokenOnRequests() {
        CapturingRequester requester = new CapturingRequester();
        UnbelievaBoatClient client = new UnbelievaBoatClient("raw-token", requester);

        BankAccount account = client.get(456, 123);

        assertNotNull(account);
        assertEquals("raw-token", requester.request.header("Authorization"));
        assertEquals("https://unbelievaboat.com/api/v1/guilds/123/users/456", requester.request.url().toString());
        assertEquals("GET", requester.request.method());
    }

    @Test
    void rejectsBlankTokens() {
        assertThrows(IllegalArgumentException.class, () -> new UnbelievaBoatClient(""));
    }

    @Test
    void parsesInfinityValuesAsLongMaxValue() {
        BankAccount account = UnbelievaBoatClient.fromJson(123, """
                {
                  "user_id": "456",
                  "cash": "Infinity",
                  "bank": 42,
                  "total": 92233720368547758080,
                  "rank": "1"
                }
                """);

        assertNotNull(account);
        assertEquals(Long.MAX_VALUE, account.getCash());
        assertEquals(42, account.getBank());
        assertEquals(Long.MAX_VALUE, account.getTotal());
    }

    private static class CapturingRequester extends UnbelievaBoatRequester {
        private Request request;

        @Override
        public RequestMapper makeRequest(Supplier<Request> requestSupplier) throws HttpRequestException {
            this.request = requestSupplier.get();
            return new RequestMapper("""
                    {
                      "user_id": "456",
                      "cash": 10,
                      "bank": 20,
                      "total": 30,
                      "rank": "1"
                    }
                    """.getBytes(), true, 200);
        }
    }
}
