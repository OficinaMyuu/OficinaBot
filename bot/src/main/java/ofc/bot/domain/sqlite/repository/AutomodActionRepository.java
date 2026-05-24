package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.AutomodAction;
import ofc.bot.domain.tables.AutomodActionsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link AutomodAction} entity.
 */
public class AutomodActionRepository extends Repository<AutomodAction> {
    private static final AutomodActionsTable AUTOMOD_ACTIONS = AutomodActionsTable.AUTOMOD_ACTIONS;

    public AutomodActionRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<AutomodAction> getTable() {
        return AUTOMOD_ACTIONS;
    }

    public AutomodAction findLastByThreshold(int threshold) {
        return ctx.selectFrom(AUTOMOD_ACTIONS)
                .where(AUTOMOD_ACTIONS.THRESHOLD.le(threshold))
                .orderBy(AUTOMOD_ACTIONS.THRESHOLD.desc())
                .limit(1)
                .fetchOne();
    }
}
