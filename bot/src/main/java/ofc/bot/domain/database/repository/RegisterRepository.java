package ofc.bot.domain.database.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.RegisterData;
import ofc.bot.domain.tables.RegistersTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;

public class RegisterRepository extends Repository<RegisterData> {
    private static final RegistersTable REGISTERS = RegistersTable.REGISTERS;

    public RegisterRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<RegisterData> getTable() {
        return REGISTERS;
    }

    @Nullable
    public RegisterData findById(int id) {
        return ctx.selectFrom(REGISTERS)
                .where(REGISTERS.ID.eq(id))
                .fetchOne();
    }
}
