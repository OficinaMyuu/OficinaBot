package ofc.bot.commands.impl.slash.levels;

import ofc.bot.handlers.requests.RequestMapper;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LevelCardBackendClientTest {
    @Test
    void buildEndpointAppendsBackendPath() {
        String endpoint = LevelCardBackendClient.buildEndpoint("http://10.0.1.10:8080", "/api/levels/cards");

        assertEquals("http://10.0.1.10:8080/api/levels/cards", endpoint);
    }

    @Test
    void buildEndpointIgnoresTrailingBaseSlash() {
        String endpoint = LevelCardBackendClient.buildEndpoint("http://10.0.1.10:8080/", "/api/levels/roles");

        assertEquals("http://10.0.1.10:8080/api/levels/roles", endpoint);
    }

    @Test
    void buildEndpointRejectsBlankBaseUrl() {
        IllegalArgumentException err = assertThrows(
                IllegalArgumentException.class,
                () -> LevelCardBackendClient.buildEndpoint(" ", "/api/levels/cards")
        );

        assertTrue(err.getMessage().contains("blank"));
    }

    @Test
    void buildEndpointRejectsBaseWithoutHttpScheme() {
        IllegalArgumentException err = assertThrows(
                IllegalArgumentException.class,
                () -> LevelCardBackendClient.buildEndpoint("10.0.1.10:8080", "/api/levels/cards")
        );

        assertTrue(err.getMessage().contains("HTTP"));
    }

    @Test
    void sendCardRequestPostsJsonWithoutLegacyApiKey() {
        AtomicReference<Request> sentRequest = new AtomicReference<>();
        byte[] expected = new byte[]{1, 2, 3};
        String image = Base64.getEncoder().encodeToString(expected);

        byte[] actual = LevelCardBackendClient.sendCardRequest(
                "http://10.0.1.10:8080/api/levels/cards",
                new TestPayload("Myuu"),
                requestSupplier -> {
                    Request request = requestSupplier.get();
                    sentRequest.set(request);
                    return new RequestMapper(("{\"image\":\"" + image + "\"}").getBytes(), true, 200);
                }
        );

        Request request = sentRequest.get();
        assertArrayEquals(expected, actual);
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertEquals("http://10.0.1.10:8080/api/levels/cards", request.url().toString());
        assertNull(request.header("x-api-key"));
        assertNotNull(request.body());
    }

    private record TestPayload(String username) {}
}
