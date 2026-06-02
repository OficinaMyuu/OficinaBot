package ofc.bot.handlers.requests.requester.impl;

import ofc.bot.handlers.requests.RequestMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UnbelievaBoatRequesterTest {
    private static final MediaType JSON = MediaType.parse("application/json");

    @Test
    void retriesRateLimitedRequestUsingRetryAfterFromBody() {
        List<Long> sleeps = new ArrayList<>();
        OkHttpClient client = newClient(
                response(
                        429,
                        "{\"message\":\"You are being rate limited\",\"retry_after\":1234}",
                        new Headers.Builder()
                                .add("X-RateLimit-Remaining", "0")
                                .add("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + 5000))
                                .build()
                ),
                response(200, "{\"ok\":true}")
        );
        UnbelievaBoatRequester requester = new UnbelievaBoatRequester(client, sleeps::add, 2, 100);

        RequestMapper mapper = requester.makeRequest(() -> new Request.Builder()
                .url("https://unbelievaboat.com/api/v1/guilds/1/users/2")
                .build());

        assertTrue(mapper.isOk());
        assertEquals(200, mapper.getStatusCode());
        assertEquals(List.of(1234L), sleeps);
    }

    @Test
    void waitsUntilResetWhenResponseIsNearRateLimit() {
        List<Long> sleeps = new ArrayList<>();
        long resetAt = System.currentTimeMillis() + 500;
        OkHttpClient client = newClient(response(
                200,
                "{\"ok\":true}",
                new Headers.Builder()
                        .add("X-RateLimit-Remaining", "1")
                        .add("X-RateLimit-Reset", String.valueOf(resetAt))
                        .build()
        ));
        UnbelievaBoatRequester requester = new UnbelievaBoatRequester(client, sleeps::add, 1, 100);

        RequestMapper mapper = requester.makeRequest(() -> new Request.Builder()
                .url("https://unbelievaboat.com/api/v1/guilds/1/users/2")
                .build());

        assertTrue(mapper.isOk());
        assertEquals(1, sleeps.size());
        assertTrue(sleeps.getFirst() > 0);
        assertTrue(sleeps.getFirst() <= 500);
    }

    @Test
    void fallsBackToExponentialBackoffWhenRetryAfterIsUnavailable() {
        List<Long> sleeps = new ArrayList<>();
        OkHttpClient client = newClient(
                response(429, "{\"global\":true}"),
                response(200, "{\"ok\":true}")
        );
        UnbelievaBoatRequester requester = new UnbelievaBoatRequester(client, sleeps::add, 2, 100);

        requester.makeRequest(() -> new Request.Builder()
                .url("https://unbelievaboat.com/api/v1/guilds/1/users/2")
                .build());

        assertEquals(List.of(100L), sleeps);
    }

    private OkHttpClient newClient(QueuedResponse... responses) {
        List<QueuedResponse> queue = new ArrayList<>(List.of(responses));

        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    if (queue.isEmpty()) {
                        throw new IOException("No queued response for " + chain.request().url());
                    }
                    return queue.removeFirst().toResponse(chain.request());
                })
                .build();
    }

    private static QueuedResponse response(int code, String body) {
        return response(code, body, Headers.of());
    }

    private static QueuedResponse response(int code, String body, Headers headers) {
        return new QueuedResponse(code, body, headers);
    }

    private record QueuedResponse(int code, String body, Headers headers) {
        Response toResponse(@NotNull Request request) {
            return new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(this.code)
                    .message(this.code >= 200 && this.code < 300 ? "OK" : "Too Many Requests")
                    .headers(this.headers)
                    .body(ResponseBody.create(this.body, JSON))
                    .build();
        }
    }
}
