package ofc.bot.commands.impl.slash.levels;

import net.dv8tion.jda.api.utils.data.DataObject;
import ofc.bot.handlers.requests.RequestMapper;
import ofc.bot.handlers.requests.Route;
import ofc.bot.handlers.requests.requester.Requester;
import ofc.bot.util.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Locale;

final class LevelCardBackendClient {
    static final String BACKEND_BASE_URL_KEY = "backend.api.base-url";
    private static final Logger LOGGER = LoggerFactory.getLogger(LevelCardBackendClient.class);
    private static final String RANK_CARD_PATH = "/levels/cards";
    private static final String ROLES_CARD_PATH = "/levels/roles";
    private static final String IMAGE_CONTENT_TYPE = "image/png";

    private LevelCardBackendClient() {}

    static byte[] createRankCard(Object data) {
        return sendCardRequest(RANK_CARD_PATH, data);
    }

    static byte[] createRolesCard(DataObject payload) {
        return sendCardRequest(ROLES_CARD_PATH, payload);
    }

    static String buildEndpoint(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Backend base URL is blank");
        }
        if (path == null || path.isBlank() || path.charAt(0) != '/') {
            throw new IllegalArgumentException("Backend path must start with /");
        }

        String endpoint = stripTrailingSlashes(baseUrl.strip()) + path;
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Backend base URL must be an absolute HTTP(S) URL", e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || uri.getHost() == null || !isHttpScheme(scheme)) {
            throw new IllegalArgumentException("Backend base URL must be an absolute HTTP(S) URL");
        }
        return uri.toString();
    }

    private static byte[] sendCardRequest(String path, Object payload) {
        String endpoint = resolveEndpoint(path);
        if (endpoint == null) {
            return new byte[0];
        }

        RequestMapper result = Route.post(endpoint).create()
                .addHeader("Accept", IMAGE_CONTENT_TYPE)
                .setBody(payload)
                .send();

        return decodeCardResponse(result);
    }

    static byte[] sendCardRequest(String endpoint, Object payload, Requester requester) {
        RequestMapper result = Route.post(endpoint).create()
                .addHeader("Accept", IMAGE_CONTENT_TYPE)
                .setBody(payload)
                .send(requester);

        return decodeCardResponse(result);
    }

    private static byte[] decodeCardResponse(RequestMapper result) {
        if (result.getStatusCode() != 200)
            return new byte[0];

        return result.asBytes();
    }

    private static String resolveEndpoint(String path) {
        String baseUrl = Bot.get(BACKEND_BASE_URL_KEY);
        try {
            return buildEndpoint(baseUrl, path);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid backend card API config {}={}", BACKEND_BASE_URL_KEY, baseUrl, e);
            return null;
        }
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean isHttpScheme(String scheme) {
        String normalized = scheme.toLowerCase(Locale.ROOT);
        return normalized.equals("http") || normalized.equals("https");
    }
}
