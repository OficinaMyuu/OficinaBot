package ofc.bot.internal.data;

import ofc.bot.domain.sqlite.DB;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public final class BotProperties {
    public static final long DEV_ID = 596939790532739075L;
    private static final Logger LOGGER = LoggerFactory.getLogger(BotProperties.class);
    private static final Map<String, String> data = new HashMap<>();

    private BotProperties() {}

    /**
     * Gets the value associated with the specified key from the cache.
     * <p>
     * <b>Note:</b> This method only checks for cached values. For values not stored in-memory,
     * use {@link #find(String)} (or {@link #fetch(String)} to force a database call) instead.
     * </p>
     *
     * @param key The key whose associated value is to be returned.
     * @return The value to which the specified key is mapped.
     * @see #find(String)
     */
    @Nullable
    public static String get(String key) {
        return data.get(key);
    }

    /**
     * Retrieves the value associated with the specified key. This method first checks the cache for the value.
     * If the value is not found in the cache (or the key maps to {@code null}), it queries the database.
     *
     * @param key The key whose associated value is to be returned.
     * @return The value to which the specified key is mapped, or {@code null} if the key is not found in both places.
     * @throws DataAccessException If something went wrong executing the query.
     */
    @Nullable
    public static String find(String key) throws DataAccessException {
        String value = get(key);
        return value == null ? fetch(key) : value;
    }

    public static String fetch(String key) throws DataAccessException {
        DSLContext ctx = DB.getContext();
        String value = ctx.select(field("value"))
                .from(table("config"))
                .where(field("key").eq(key))
                .fetchOneInto(String.class);

        if (value == null)
            LOGGER.warn("Found no values for key \"{}\"", key);

        data.put(key, value);
        return value;
    }
}