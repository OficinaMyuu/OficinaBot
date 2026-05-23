package ofc.bot.internal;

import ofc.bot.database.DB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class BotData {
    public static final String PREFIX = "r!";
    private static final Map<String, String> cache = new HashMap<>();

    @NotNull
    public static <T> T getSafe(String key, Function<String, T> mapper) {
        T value = get(key, mapper);
        if (value == null)
            throw new NoSuchElementException("Found no values for key " + key);

        return value;
    }

    @Nullable
    public static <T> T get(String key, Function<String, T> mapper) {
        String value = get(key);
        return value == null ? null : mapper.apply(value);
    }

    @Nullable
    public static String get(String key) {
        String value = cache.get(key);
        return value == null ? fetch(key) : value;
    }

    private static <T> T fetch(String key, Function<String, T> mapper) {
        String result = fetch(key);
        return result == null ? null : mapper.apply(result);
    }

    private static String fetch(String key) {
        DSLContext ctx = DB.context();

        String value = ctx.select(field("value"))
                .from(table("config"))
                .where(field("key").eq(key))
                .fetchOneInto(String.class);

        cache.put(key, value);
        return value;
    }
}