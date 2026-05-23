package ofc.bot.database;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.TableImpl;

import static org.jooq.impl.DSL.name;

public abstract class AbstractTable<K, R extends Record> extends TableImpl<R> {

    public AbstractTable(String name) {
        super(name(name));
    }

    public abstract Field<K> getIdKey();
}