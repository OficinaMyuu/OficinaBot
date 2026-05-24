package ofc.bot.database.repositories;

import ofc.bot.database.DB;
import ofc.bot.database.Repository;
import ofc.bot.database.models.RegisterData;
import org.jooq.DSLContext;

public class RegistersRepository implements Repository<Integer, RegisterData.Table, RegisterData> {
    private static final RegistersRepository INSTANCE = new RegistersRepository();

    private RegistersRepository() {}

    public static RegistersRepository getInstance() {
        return INSTANCE;
    }

    @Override
    public RegisterData.Table getTable() {
        return RegisterData.Table.REGISTER_DATA;
    }

    @Override
    public RegisterData findById(Integer id) {

        DSLContext ctx = DB.context();

        return ctx.selectFrom(getTable())
                .where(getTable().getIdKey().eq(id))
                .fetchOne();
    }
}