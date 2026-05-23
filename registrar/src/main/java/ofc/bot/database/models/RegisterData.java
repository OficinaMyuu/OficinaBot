package ofc.bot.database.models;

import net.dv8tion.jda.api.entities.Member;
import ofc.bot.database.AbstractTable;
import ofc.bot.utils.Roles;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableRecordImpl;

import static ofc.bot.database.models.RegisterData.Table.REGISTER_DATA;
import static org.jooq.impl.DSL.name;

public class RegisterData extends TableRecordImpl<RegisterData> {

    public RegisterData() {
        super(REGISTER_DATA);
    }

    public RegisterData(Roles gender, Roles device, int age, long targetId, long moderatorId) {
        this();
        set(REGISTER_DATA.TARGET_ID, targetId);
        set(REGISTER_DATA.MODERATOR_ID, moderatorId);
        set(REGISTER_DATA.GENDER, gender.name());
        set(REGISTER_DATA.DEVICE, device.name());
        set(REGISTER_DATA.AGE, age);
    }

    public RegisterData(Roles gender, Roles device, int age, Member target, Member moderator) {
        this(gender, device, age, target.getIdLong(), moderator.getIdLong());
    }

    public Roles getGender() {
        return Roles.valueOf(get(REGISTER_DATA.GENDER));
    }

    public Roles getDevice() {
        return Roles.valueOf(get(REGISTER_DATA.DEVICE));
    }

    public long getTargetId() {
        return get(REGISTER_DATA.TARGET_ID);
    }

    public long getModeratorId() {
        return get(REGISTER_DATA.MODERATOR_ID);
    }

    public int getAge() {
        return get(REGISTER_DATA.AGE);
    }

    public long getTimeCreated() {
        return get(REGISTER_DATA.CREATED_AT);
    }

    public static class Table extends AbstractTable<Integer, RegisterData> {
        public static final Table REGISTER_DATA = new Table();

        public final Field<Integer> ID        = createField(name("id"),           SQLDataType.INTEGER.identity(true));
        public final Field<Long> TARGET_ID    = createField(name("target_id"),    SQLDataType.BIGINT.notNull());
        public final Field<Long> MODERATOR_ID = createField(name("moderator_id"), SQLDataType.BIGINT.notNull());
        public final Field<Integer> AGE       = createField(name("age"),          SQLDataType.INTEGER.notNull());
        public final Field<String> GENDER     = createField(name("gender"),       SQLDataType.CHAR.notNull());
        public final Field<String> DEVICE     = createField(name("device"),       SQLDataType.CHAR.notNull());
        public final Field<Long> CREATED_AT   = createField(name("created_at"),   SQLDataType.BIGINT.notNull());

        private Table() {
            super("registers");
        }

        @Override
        public Field<Integer> getIdKey() {
            return ID;
        }
    }
}