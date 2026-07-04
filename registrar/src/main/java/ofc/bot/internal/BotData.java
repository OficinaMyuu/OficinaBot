package ofc.bot.internal;

import ofc.bot.database.DB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.quotedName;
import static org.jooq.impl.DSL.table;

public class BotData {
    public static final String PREFIX = "r!";
    private static final Map<String, String> cache = new HashMap<>();
    private static final Table<?> CONFIG_TABLE = table(quotedName("config"));
    private static final Field<String> CONFIG_KEY = field(quotedName("key"), String.class);
    private static final Field<String> CONFIG_VALUE = field(quotedName("value"), String.class);

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

        String value = selectConfigValue(ctx, key).fetchOneInto(String.class);

        cache.put(key, value);
        return value;
    }

    static SelectConditionStep<Record1<String>> selectConfigValue(DSLContext ctx, String key) {
        return ctx.select(CONFIG_VALUE)
                .from(CONFIG_TABLE)
                .where(CONFIG_KEY.eq(key));
    }
}
