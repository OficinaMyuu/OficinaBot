package ofc.bot.handlers.requests.requester.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ofc.bot.handlers.requests.RequestMapper;
import ofc.bot.handlers.requests.exceptions.HttpRequestException;
import ofc.bot.handlers.requests.requester.Requester;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class UnbelievaBoatRequester implements Requester {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnbelievaBoatRequester.class);
    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient();
    private static final Gson GSON = new Gson();
    private static final int RATE_LIMIT_CODE = 429;
    private static final int MAX_RETRIES = 5;
    private static final long DEFAULT_DELAY_MS = 1000;
    private final OkHttpClient client;
    private final Sleeper sleeper;
    private final int maxRetries;
    private final long defaultDelayMs;

    /**
     * Creates a requester configured for UnbelievaBoat's API retry behavior.
     */
    public UnbelievaBoatRequester() {
        this(DEFAULT_CLIENT, Thread::sleep, MAX_RETRIES, DEFAULT_DELAY_MS);
    }

    UnbelievaBoatRequester(OkHttpClient client, Sleeper sleeper, int maxRetries, long defaultDelayMs) {
        this.client = client;
        this.sleeper = sleeper;
        this.maxRetries = maxRetries;
        this.defaultDelayMs = defaultDelayMs;
    }

    /**
     * Executes a request and retries HTTP 429 responses using UnbelievaBoat's rate-limit metadata.
     *
     * @param supplier supplies a fresh OkHttp request for each attempt.
     * @return the final response body, status flag, and HTTP status code.
     * @throws HttpRequestException when the request fails, the retry budget is exhausted,
     *                              or the retry sleep is interrupted.
     */
    @NotNull
    @Override
    public RequestMapper makeRequest(@NotNull Supplier<Request> supplier) throws HttpRequestException {
        int attempt = 0;

        while (attempt < this.maxRetries) {
            attempt++;

            Request req = supplier.get();
            try (Response resp = this.client.newCall(req).execute()) {
                int code = resp.code();
                byte[] bytes = readBody(resp);

                if (!resp.isSuccessful()) {
                    LOGGER.warn("UnbelievaBoat request to \"{}\" returned HTTP {}: {}",
                            req.url(), code, new String(bytes, StandardCharsets.UTF_8));
                }

                if (code != RATE_LIMIT_CODE) {
                    delayIfApproachingRateLimit(resp.headers());
                    return new RequestMapper(bytes, resp.isSuccessful(), code);
                }

                long delay = getRetryDelay(resp.headers(), bytes, attempt);
                LOGGER.warn("Received HTTP 429 (rate limited) on attempt {} of {}. Retrying after {}ms.",
                        attempt, this.maxRetries, delay);

                sleep(delay);
            } catch (IOException e) {
                throw new HttpRequestException(e);
            }
        }
        throw new HttpRequestException("Max retry attempts reached due to rate limiting.");
    }

    private byte[] readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        return body == null ? new byte[0] : body.bytes();
    }

    private void delayIfApproachingRateLimit(Headers headers) {
        String remainingStr = headers.get("X-RateLimit-Remaining");
        if (remainingStr == null) {
            return;
        }

        try {
            int remaining = Integer.parseInt(remainingStr);
            long delay = getResetDelay(headers);
            if (remaining <= 1 && delay > 0) {
                LOGGER.warn("Approaching UnbelievaBoat rate limit: only {} requests remaining. Delaying {}ms until reset.",
                        remaining, delay);
                sleep(delay);
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse UnbelievaBoat rate-limit headers.", e);
        }
    }

    private long getRetryDelay(Headers headers, byte[] body, int attempt) {
        Long retryAfter = getRetryAfterFromBody(body);
        if (retryAfter != null && retryAfter > 0) {
            return retryAfter;
        }

        long resetDelay = getResetDelay(headers);
        if (resetDelay > 0) {
            return resetDelay;
        }

        return getBackoffDelay(attempt);
    }

    private Long getRetryAfterFromBody(byte[] body) {
        if (body.length == 0) {
            return null;
        }

        try {
            String json = new String(body, StandardCharsets.UTF_8);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);

            if (obj == null) {
                return null;
            }

            if (obj.has("global") && obj.get("global").getAsBoolean()) {
                LOGGER.warn("Global UnbelievaBoat rate limit encountered.");
            }

            return obj.has("retry_after") ? obj.get("retry_after").getAsLong() : null;
        } catch (Exception e) {
            LOGGER.warn("Could not parse retry_after from UnbelievaBoat 429 response body.", e);
            return null;
        }
    }

    private long getResetDelay(Headers headers) {
        String resetStr = headers.get("X-RateLimit-Reset");
        if (resetStr == null) {
            return 0;
        }

        try {
            return Long.parseLong(resetStr) - System.currentTimeMillis();
        } catch (NumberFormatException e) {
            LOGGER.warn("Could not parse UnbelievaBoat X-RateLimit-Reset header.", e);
            return 0;
        }
    }

    private long getBackoffDelay(int attempt) {
        return this.defaultDelayMs * (1L << Math.max(0, attempt - 1));
    }

    private void sleep(long delayMs) {
        long delay = delayMs > 0 ? delayMs : this.defaultDelayMs;

        try {
            this.sleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRequestException(e);
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long delayMs) throws InterruptedException;
    }
}
