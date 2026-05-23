package ofc.bot.database;

import ofc.bot.utils.BotUtil;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertSetMoreStep;
import org.jooq.Record;

import java.util.HashMap;
import java.util.Map;

import static org.jooq.impl.DSL.field;

public interface Repository<K, T extends AbstractTable<K, R>, R extends Record> {

    T getTable();

    R findById(K id);

    default void save(R entity) {
        save(entity, true);
    }

    default void save(R entity, boolean override) {

        DSLContext ctx = DB.context();
        Map<Field<?>, Object> values = getValues(entity);
        InsertSetMoreStep<R> insert = ctx.insertInto(getTable())
                .set(values)
                .set(field("created_at"), BotUtil.unixNow());

        if (override)
            insert.onDuplicateKeyUpdate().set(values).execute();
        else
            insert.onDuplicateKeyIgnore().execute();
    }

    private Map<Field<?>, Object> getValues(R rec) {

        Field<?>[] fields = rec.fields();
        Map<Field<?>, Object> mappedValues = new HashMap<>(fields.length);

        for (Field<?> f : fields) {

            // Skip fields that are auto-increment (identity) and have a null value
            if (f.getDataType().identity() && f.getValue(rec) == null)
                continue;

            if (!f.getName().equals("created_at"))
                mappedValues.put(f, f.get(rec));
        }

        return mappedValues;
    }
}