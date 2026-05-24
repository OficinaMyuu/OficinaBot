package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.GameParticipant;
import ofc.bot.domain.tables.GamesParticipantsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

/**
 * Repository for {@link GameParticipant} entity.
 */
public class GameParticipantRepository extends Repository<GameParticipant> {
    private static final GamesParticipantsTable GAMES_PARTICIPANTS = GamesParticipantsTable.GAMES_PARTICIPANTS;

    public GameParticipantRepository(@NotNull DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<GameParticipant> getTable() {
        return GAMES_PARTICIPANTS;
    }
}