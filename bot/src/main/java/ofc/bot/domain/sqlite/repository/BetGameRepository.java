package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.BetGame;
import ofc.bot.domain.tables.BetGamesTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link BetGame} entity.
 */
public class BetGameRepository extends Repository<BetGame> {
    private static final BetGamesTable BET_GAMES = BetGamesTable.BET_GAMES;

    public BetGameRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<BetGame> getTable() {
        return BET_GAMES;
    }
}