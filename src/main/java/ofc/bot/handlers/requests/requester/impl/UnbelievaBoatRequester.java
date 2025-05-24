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
import java.util.function.Supplier;

public class UnbelievaBoatRequester implements Requester {
    private static final HttpRequestException MAX_RETRIES_EXCEPTION = new HttpRequestException(
            "Max retry attempts reached due to rate limiting.");
    private static final Logger LOGGER = LoggerFactory.getLogger(UnbelievaBoatRequester.class);
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final int RATE_LIMIT_CODE = 429;
    private static final int MAX_RETRIES = 5;
    private static final long DEFAULT_DELAY_MS = 1000;

    @NotNull
    @Override
    public RequestMapper makeRequest(@NotNull Supplier<Request> supplier) throws HttpRequestException {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
            attempt++;

            Request req = supplier.get();
            try (Response resp = client.newCall(req).execute()) {
                ResponseBody body = resp.body();
                int code = resp.code();
                byte[] bytes = body == null ? new byte[0] : body.bytes();

                checkRateLimitHeaders(resp.headers());

                if (!resp.isSuccessful()) {
                    LOGGER.warn("We did not receive a successful status code {} from the server: {}", code, new String(bytes));
                }

                if (code != RATE_LIMIT_CODE) {
                    return new RequestMapper(bytes, resp.isSuccessful(), code);
                }

                // We hit a rate-limit, nooo :/
                long delay = getDelayFromResponse(resp, attempt);
                LOGGER.warn("Received HTTP 429 (rate limited) on attempt {} of {}. Retrying (maybe) after {}ms...",
                        attempt, MAX_RETRIES, delay);

                Thread.sleep(delay);
            } catch (IOException | InterruptedException e) {
                throw new HttpRequestException(e);
            }
        }
        throw MAX_RETRIES_EXCEPTION;
    }

    private void checkRateLimitHeaders(Headers headers) {
        String remainingStr = headers.get("X-RateLimit-Remaining");
        String resetStr = headers.get("X-RateLimit-Reset");

        if (remainingStr != null && resetStr != null) {
            try {
                int remaining = Integer.parseInt(remainingStr);
                long resetEpochMs = Long.parseLong(resetStr);
                if (remaining <= 1) {
                    long delay = resetEpochMs - System.currentTimeMillis();
                    if (delay > 0) {
                        LOGGER.warn("Approaching rate limit: only {} requests remaining. Delaying {}ms until reset.", remaining, delay);
                        Thread.sleep(delay);
                    }
                }
            } catch (NumberFormatException | InterruptedException e) {
                LOGGER.warn("Error parsing rate limit headers.", e);
            }
        }
    }

    private long getDelayFromResponse(Response resp, int attempt) {
        long delay = DEFAULT_DELAY_MS;

        try (ResponseBody body = resp.body()) {
            if (body != null) {
                String json = body.string();
                JsonObject obj = gson.fromJson(json, JsonObject.class);

                if (obj != null && obj.has("retry_after")) {
                    delay = obj.get("retry_after").getAsLong();
                }

                if (obj != null && obj.has("global") && obj.get("global").getAsBoolean()) {
                    LOGGER.warn("Global rate limit encountered");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not parse retry_after from the 429 response, falling back to default delay.", e);
            // Fall back to an exponential backoff
            delay = DEFAULT_DELAY_MS * (1L << attempt);
        }

        // Fallback if delay is negative
        return delay > 0 ? delay : DEFAULT_DELAY_MS * (1L << attempt);
    }
}
