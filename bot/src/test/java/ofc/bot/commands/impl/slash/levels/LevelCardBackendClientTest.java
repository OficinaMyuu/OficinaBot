package ofc.bot.commands.impl.slash.levels;

import ofc.bot.handlers.requests.RequestMapper;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LevelCardBackendClientTest {
    @Test
    void buildEndpointAppendsBackendPath() {
        String endpoint = LevelCardBackendClient.buildEndpoint("http://10.0.1.10:8080", "/levels/cards");

        assertEquals("http://10.0.1.10:8080/levels/cards", endpoint);
    }

    @Test
    void buildEndpointIgnoresTrailingBaseSlash() {
        String endpoint = LevelCardBackendClient.buildEndpoint("http://10.0.1.10:8080/", "/levels/roles");

        assertEquals("http://10.0.1.10:8080/levels/roles", endpoint);
    }

    @Test
    void buildEndpointRejectsBlankBaseUrl() {
        IllegalArgumentException err = assertThrows(
                IllegalArgumentException.class,
                () -> LevelCardBackendClient.buildEndpoint(" ", "/levels/cards")
        );

        assertTrue(err.getMessage().contains("blank"));
    }

    @Test
    void buildEndpointRejectsBaseWithoutHttpScheme() {
        IllegalArgumentException err = assertThrows(
                IllegalArgumentException.class,
                () -> LevelCardBackendClient.buildEndpoint("10.0.1.10:8080", "/levels/cards")
        );

        assertTrue(err.getMessage().contains("HTTP"));
    }

    @Test
    void sendCardRequestPostsJsonAndAcceptsRawImageWithoutLegacyApiKey() {
        AtomicReference<Request> sentRequest = new AtomicReference<>();
        byte[] expected = new byte[]{1, 2, 3};

        byte[] actual = LevelCardBackendClient.sendCardRequest(
                "http://10.0.1.10:8080/levels/cards",
                new TestPayload("Myuu"),
                requestSupplier -> {
                    Request request = requestSupplier.get();
                    sentRequest.set(request);
                    return new RequestMapper(expected, true, 200);
                }
        );

        Request request = sentRequest.get();
        assertArrayEquals(expected, actual);
        assertNotNull(request);
        assertEquals("POST", request.method());
        assertEquals("http://10.0.1.10:8080/levels/cards", request.url().toString());
        assertEquals("image/png", request.header("Accept"));
        assertNull(request.header("x-api-key"));
        assertNotNull(request.body());
    }

    private record TestPayload(String username) {}
}
