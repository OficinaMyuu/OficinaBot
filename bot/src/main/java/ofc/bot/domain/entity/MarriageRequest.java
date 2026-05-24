package ofc.bot.domain.entity;

import ofc.bot.domain.tables.MarriageRequestsTable;
import ofc.bot.util.Bot;

public class MarriageRequest extends OficinaRecord<MarriageRequest> {
    private static final MarriageRequestsTable MARRIAGE_REQUESTS = MarriageRequestsTable.MARRIAGE_REQUESTS;

    public MarriageRequest() {
        super(MARRIAGE_REQUESTS);
    }

    public MarriageRequest(long reqId, long tarId, long createdAt) {
        this();
        set(MARRIAGE_REQUESTS.REQUESTER_ID, reqId);
        set(MARRIAGE_REQUESTS.TARGET_ID, tarId);
        set(MARRIAGE_REQUESTS.CREATED_AT, createdAt);
    }

    public static MarriageRequest fromUsers(long reqId, long tarId) {
        return new MarriageRequest(reqId, tarId, Bot.unixNow());
    }

    public int getId() {
        return get(MARRIAGE_REQUESTS.ID);
    }

    public long getRequesterId() {
        return get(MARRIAGE_REQUESTS.REQUESTER_ID);
    }

    public long getTargetId() {
        return get(MARRIAGE_REQUESTS.TARGET_ID);
    }

    public long getTimeCreated() {
        return get(MARRIAGE_REQUESTS.CREATED_AT);
    }

    public MarriageRequest setRequesterId(long reqId) {
        set(MARRIAGE_REQUESTS.REQUESTER_ID, reqId);
        return this;
    }

    public MarriageRequest setTargetId(long targetId) {
        set(MARRIAGE_REQUESTS.TARGET_ID, targetId);
        return this;
    }
}