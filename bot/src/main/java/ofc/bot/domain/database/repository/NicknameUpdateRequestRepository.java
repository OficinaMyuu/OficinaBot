package ofc.bot.domain.database.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.NicknameUpdateRequest;
import ofc.bot.domain.entity.enums.NicknameRequestStatus;
import ofc.bot.domain.tables.NicknameUpdateRequestsTable;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

public class NicknameUpdateRequestRepository extends Repository<NicknameUpdateRequest> {
    private static final NicknameUpdateRequestsTable NICK_REQUESTS =
            NicknameUpdateRequestsTable.NICKNAME_UPDATE_REQUESTS;

    public NicknameUpdateRequestRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<NicknameUpdateRequest> getTable() {
        return NICK_REQUESTS;
    }

    public NicknameUpdateRequest findByRequestId(@NotNull String requestId) {
        return ctx.selectFrom(NICK_REQUESTS)
                .where(NICK_REQUESTS.REQUEST_ID.eq(requestId))
                .fetchOne();
    }

    public NicknameUpdateRequest findByButtonId(@NotNull String buttonId) {
        return ctx.selectFrom(NICK_REQUESTS)
                .where(NICK_REQUESTS.APPROVE_BUTTON_ID.eq(buttonId)
                        .or(NICK_REQUESTS.REJECT_BUTTON_ID.eq(buttonId)))
                .fetchOne();
    }

    public boolean startProcessing(@NotNull String requestId, long actorId, long updatedAt) {
        return updateStatus(
                requestId,
                NicknameRequestStatus.PENDING,
                NicknameRequestStatus.PROCESSING,
                actorId,
                null,
                updatedAt
        );
    }

    public boolean markApproved(@NotNull String requestId, long actorId, long decidedAt) {
        return updateStatus(
                requestId,
                NicknameRequestStatus.PROCESSING,
                NicknameRequestStatus.APPROVED,
                actorId,
                decidedAt,
                decidedAt
        );
    }

    public boolean markRejected(@NotNull String requestId, long actorId, long decidedAt) {
        return updateStatus(
                requestId,
                NicknameRequestStatus.PENDING,
                NicknameRequestStatus.REJECTED,
                actorId,
                decidedAt,
                decidedAt
        );
    }

    public void markFailed(@NotNull String requestId, long actorId, long decidedAt) {
        ctx.update(NICK_REQUESTS)
                .set(NICK_REQUESTS.STATUS, NicknameRequestStatus.FAILED.name())
                .set(NICK_REQUESTS.DECISION_AUTHOR_ID, actorId)
                .set(NICK_REQUESTS.DECIDED_AT, decidedAt)
                .set(NICK_REQUESTS.UPDATED_AT, decidedAt)
                .where(NICK_REQUESTS.REQUEST_ID.eq(requestId))
                .execute();
    }

    private boolean updateStatus(
            String requestId,
            NicknameRequestStatus expected,
            NicknameRequestStatus next,
            long actorId,
            Long decidedAt,
            long updatedAt
    ) {
        return ctx.update(NICK_REQUESTS)
                .set(NICK_REQUESTS.STATUS, next.name())
                .set(NICK_REQUESTS.DECISION_AUTHOR_ID, actorId)
                .set(NICK_REQUESTS.DECIDED_AT, decidedAt)
                .set(NICK_REQUESTS.UPDATED_AT, updatedAt)
                .where(NICK_REQUESTS.REQUEST_ID.eq(requestId))
                .and(NICK_REQUESTS.STATUS.eq(expected.name()))
                .execute() == 1;
    }
}
