package ofc.bot.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds and mutates URLs while preserving the original scheme, authority, path, and fragment.
 * <p>
 * Query parameters are stored as decoded single-value entries in insertion order.
 * Re-adding an existing key replaces its value while keeping the latest position in the map.
 * <p>
 * This class is intentionally single-value only. If a future use case needs repeated query
 * parameters such as {@code tag=a&tag=b}, that should be modeled explicitly in a separate API
 * instead of quietly overloading this one.
 */
public final class UrlBuilder {
    private final URI baseUri;
    private final Map<String, String> parameters = new LinkedHashMap<>();

    /**
     * Creates a builder from the provided URL string.
     *
     * @param url the absolute or relative URL to parse
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     */
    public UrlBuilder(String url) {
        this(parseUri(url));
    }

    /**
     * Creates a builder from the provided URI.
     *
     * @param uri the URI to parse
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public UrlBuilder(URI uri) {
        this.baseUri = Objects.requireNonNull(uri, "uri");
        parseQuery(uri.getRawQuery());
    }

    /**
     * Creates a builder from the provided URL string.
     *
     * @param url the URL to parse
     * @return a new builder for the provided URL
     */
    public static UrlBuilder from(String url) {
        return new UrlBuilder(url);
    }

    /**
     * Creates a builder from the provided URI.
     *
     * @param uri the URI to parse
     * @return a new builder for the provided URI
     */
    public static UrlBuilder from(URI uri) {
        return new UrlBuilder(uri);
    }

    /**
     * Sets or replaces a query parameter.
     * <p>
     * Passing {@code null} as the value removes the parameter, which is usually the least
     * surprising behavior for callers and avoids serializing the literal string {@code "null"}.
     *
     * @param key the parameter name
     * @param value the parameter value, or {@code null} to remove the parameter
     * @return this builder instance
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public UrlBuilder set(String key, String value) {
        String normalizedKey = requireKey(key);
        if (value == null) {
            parameters.remove(normalizedKey);
            return this;
        }

        parameters.put(normalizedKey, value);
        return this;
    }

    /**
     * Alias for {@link #set(String, String)} kept for fluent readability in call sites.
     *
     * @param key the parameter name
     * @param value the parameter value, or {@code null} to remove the parameter
     * @return this builder instance
     */
    public UrlBuilder add(String key, String value) {
        return set(key, value);
    }

    /**
     * Removes a query parameter if present.
     *
     * @param key the parameter name
     * @return this builder instance
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public UrlBuilder remove(String key) {
        parameters.remove(requireKey(key));
        return this;
    }

    /**
     * Removes every query parameter from this builder.
     *
     * @return this builder instance
     */
    public UrlBuilder clear() {
        parameters.clear();
        return this;
    }

    /**
     * Returns the decoded value for the provided parameter key.
     *
     * @param key the parameter name
     * @return the stored value, or {@code null} if the key does not exist
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public String get(String key) {
        return parameters.get(requireKey(key));
    }

    /**
     * Checks whether the provided parameter key exists.
     *
     * @param key the parameter name
     * @return {@code true} if the key exists, otherwise {@code false}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public boolean contains(String key) {
        return parameters.containsKey(requireKey(key));
    }

    /**
     * Returns whether the builder currently has no query parameters.
     *
     * @return {@code true} when no query parameters are stored
     */
    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    /**
     * Returns an immutable snapshot of the current decoded query parameters.
     *
     * @return an immutable parameter snapshot in insertion order
     */
    public Map<String, String> parameters() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

    /**
     * Builds the current state into a URI instance.
     *
     * @return the built URI
     * @throws IllegalStateException if the resulting URI is unexpectedly invalid
     */
    public URI toUri() {
        try {
            String query = buildQuery();
            return new URI(
                    baseUri.getScheme(),
                    baseUri.getAuthority(),
                    baseUri.getPath(),
                    query.isEmpty() ? null : query,
                    baseUri.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to build URI from builder state", e);
        }
    }

    /**
     * Builds the current state into a URL string.
     *
     * @return the built URL string
     */
    public String build() {
        return toUri().toString();
    }

    /**
     * Returns the current URL representation.
     *
     * @return the built URL string
     */
    @Override
    public String toString() {
        return build();
    }

    private static URI parseUri(String url) {
        Objects.requireNonNull(url, "url");
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private void parseQuery(String query) {
        if (query == null || query.isEmpty()) {
            return;
        }

        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }

            String[] keyValue = pair.split("=", 2);
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            parameters.put(key, value);
        }
    }

    private String buildQuery() {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }

            builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }

        return builder.toString();
    }

    private String requireKey(String key) {
        return Objects.requireNonNull(key, "key");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
