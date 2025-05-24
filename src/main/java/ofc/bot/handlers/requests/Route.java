package ofc.bot.handlers.requests;

import com.google.gson.Gson;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.requests.requester.Requester;
import ofc.bot.handlers.requests.requester.impl.DefaultRequester;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static ofc.bot.handlers.requests.Method.*;

public final class Route {
    private final Method method;
    private final String route;

    public static class UnbelievaBoat {
        private static final String BASE_URL = "https://unbelievaboat.com/api/v1/";

        // ---------- Economy ----------
        public static final Route GET_BALANCE    = new Route(GET,   BASE_URL + "guilds/%s/users/%s");
        public static final Route SET_BALANCE    = new Route(PUT,   BASE_URL + "guilds/%s/users/%s");
        public static final Route UPDATE_BALANCE = new Route(PATCH, BASE_URL + "guilds/%s/users/%s");
    }

    public static class IPs {
        public static final Route GET_IP_INFO = new Route(GET, "http://ip-api.com/json/%s");
    }

    public static class Images {
        private static final String BASE = "https://qrbe2ko4o5.execute-api.us-east-2.amazonaws.com/v1";

        public static final Route CREATE_RANK_CARD  = new Route(POST, BASE, "/levels/cards");
        public static final Route CREATE_ROLES_CARD = new Route(POST, BASE, "/levels/roles");
    }

    public Route(Method method, String route) {
        this.method = method;
        this.route = route;
    }

    public Route(Method method, String base, String path) {
        this(method, base + path);
    }

    public static Route get(String route) {
        return new Route(GET, route);
    }

    public static Route post(String route) {
        return new Route(POST, route);
    }

    public RequestBuilder create(Object... params) {
        // We must create this "safeUrl" variable, as some URLs may have the "%"
        // character on the path, which would throw exceptions on the String.format() call
        String safeUrl = this.route.replace("%", "%%");
        return new RequestBuilder(this.method, String.format(safeUrl, params));
    }

    public static class RequestBuilder {
        private static final Gson GSON = new Gson();
        private final Method method;
        private final HttpUrl.Builder urlBuilder;
        private final Map<String, String> headers;
        private String body;
        private String contentType;

        @SuppressWarnings("DataFlowIssue")
        private RequestBuilder(Method method, String route) {
            this.method = method;
            this.urlBuilder = HttpUrl.parse(route).newBuilder();
            this.headers = new HashMap<>();
            this.contentType = "application/json";
        }

        public RequestBuilder addQueryParam(String key, Object value) {
            this.urlBuilder.addQueryParameter(key, String.valueOf(value));
            return this;
        }

        public RequestBuilder setBody(String body) {
            this.body = body.strip();
            return this;
        }

        public RequestBuilder setBody(@Nullable DataObject body) {
            if (body == null) {
                this.body = null;
                return this;
            }

            this.body = body.toString();
            setContentType("application/json");
            return this;
        }

        public RequestBuilder addHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        /**
         * This is the same as turning the provided {@code obj} into
         * a JSON and calling {@link #setBody(String)}.
         *
         * @param obj the object to be turned into JSON.
         * @return the same builder instance, for chaining convenience.
         */
        public RequestBuilder setBody(Object obj) {
            return setBody(GSON.toJson(obj));
        }

        public RequestBuilder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        @NotNull
        public RequestMapper send() {
            return send(DefaultRequester.getRequester());
        }

        @NotNull
        public RequestMapper send(@NotNull Requester requester) {
            Checks.notNull(requester, "Requester");
            return requester.makeRequest(this::build);
        }

        public <T> T send(Function<RequestMapper, T> resolver) {
            return resolver.apply(this.send());
        }

        public <T> T send(Requester requester, BiFunction<RequestMapper, Integer, T> resolver) {
            RequestMapper mapper = this.send(requester);
            return resolver.apply(mapper, mapper.getStatusCode());
        }

        private Request build() {
            Request.Builder req = new Request.Builder()
                    .url(this.urlBuilder.build());

            if (this.body != null && contentType != null)
                req.method(this.method.toString(), RequestBody.create(this.body, MediaType.parse(this.contentType)));

            if (!this.headers.isEmpty())
                headers.forEach(req::addHeader);

            return req.build();
        }
    }
}