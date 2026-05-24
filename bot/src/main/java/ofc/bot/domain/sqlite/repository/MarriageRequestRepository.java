package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.abstractions.InitializableTable;
import ofc.bot.domain.entity.MarriageRequest;
import ofc.bot.domain.viewmodels.ProposalsView;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;
import org.jooq.Condition;
import org.jooq.DSLContext;

import java.util.List;

import static ofc.bot.domain.tables.MarriageRequestsTable.MARRIAGE_REQUESTS;

/**
 * Repository for {@link MarriageRequest} entity.
 */
public class MarriageRequestRepository extends Repository<MarriageRequest> {
    private static final int MAX_USERS_PER_PAGE = 10;

    public MarriageRequestRepository(DSLContext ctx) {
        super(ctx);
    }

    @NotNull
    @Override
    public InitializableTable<MarriageRequest> getTable() {
        return MARRIAGE_REQUESTS;
    }

    public boolean isPending(long reqId, long tarId) {
        return ctx.fetchExists(MARRIAGE_REQUESTS,
                MARRIAGE_REQUESTS.REQUESTER_ID.eq(reqId).and(MARRIAGE_REQUESTS.TARGET_ID.eq(tarId))
                        .or(MARRIAGE_REQUESTS.REQUESTER_ID.eq(tarId).and(MARRIAGE_REQUESTS.TARGET_ID.eq(reqId)))
        );
    }

    public List<MarriageRequest> findByUserId(Condition cond, int limit, int offset) {
        return ctx.selectFrom(MARRIAGE_REQUESTS)
                .where(cond)
                .groupBy(MARRIAGE_REQUESTS.ID)
                .orderBy(MARRIAGE_REQUESTS.CREATED_AT.desc())
                .offset(offset)
                .limit(limit)
                .fetchInto(MARRIAGE_REQUESTS);
    }

    public ProposalsView viewProposals(String type, long userId, int pageIndex) {
        MarriageRequestRepository mreqRepo = Repositories.getMarriageRequestRepository();
        Condition condition = "out".equals(type)
                ? MARRIAGE_REQUESTS.REQUESTER_ID.eq(userId)
                : MARRIAGE_REQUESTS.TARGET_ID.eq(userId);
        int offset = pageIndex * MAX_USERS_PER_PAGE;
        int rowsCount = mreqRepo.count(condition);
        List<MarriageRequest> reqs = mreqRepo.findByUserId(condition, MAX_USERS_PER_PAGE, offset);
        int maxPages = Bot.calcMaxPages(rowsCount, MAX_USERS_PER_PAGE);

        return new ProposalsView(reqs, type, userId, pageIndex + 1, maxPages, rowsCount);
    }

    public int count(Condition cond) {
        return ctx.fetchCount(MARRIAGE_REQUESTS, cond);
    }

    public void delete(MarriageRequest req) {
        ctx.executeDelete(req, MARRIAGE_REQUESTS.ID.eq(req.getId()));
    }

    public MarriageRequest findByStrictIds(long requesterId, long targetId) {
        return ctx.selectFrom(MARRIAGE_REQUESTS)
                .where(MARRIAGE_REQUESTS.REQUESTER_ID.eq(requesterId))
                .and(MARRIAGE_REQUESTS.TARGET_ID.eq(targetId))
                .fetchOne();
    }
}