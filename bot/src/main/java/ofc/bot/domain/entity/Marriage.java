package ofc.bot.domain.entity;

import ofc.bot.domain.tables.MarriagesTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

public class Marriage extends OficinaRecord<Marriage> {
    private static final MarriagesTable MARRIAGES = MarriagesTable.MARRIAGES;

    public static final String MARRIAGE_BUTTON_SCOPE = "MARRIAGES";

    public Marriage() {
        super(MARRIAGES);
    }

    public Marriage(long reqId, long tarId, long marriedAt, long createdAt, long updatedAt) {
        this();
        set(MARRIAGES.REQUESTER_ID, reqId);
        set(MARRIAGES.TARGET_ID, tarId);
        set(MARRIAGES.MARRIED_AT, marriedAt);
        set(MARRIAGES.CREATED_AT, createdAt);
        set(MARRIAGES.UPDATED_AT, updatedAt);
    }

    public static Marriage fromUsers(long reqId, long tarId) {
        long now = Bot.unixNow();
        return new Marriage(reqId, tarId, now, now, now);
    }

    public int getId() {
        return get(MARRIAGES.ID);
    }

    public long getRequesterId() {
        return get(MARRIAGES.REQUESTER_ID);
    }

    public long getTargetId() {
        return get(MARRIAGES.TARGET_ID);
    }

    public long getMarriedAt() {
        return get(MARRIAGES.MARRIED_AT);
    }

    public long getTimeCreated() {
        return get(MARRIAGES.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(MARRIAGES.UPDATED_AT);
    }

    public Marriage setRequesterId(long reqId) {
        set(MARRIAGES.REQUESTER_ID, reqId);
        return this;
    }

    public Marriage setTargetId(long targetId) {
        set(MARRIAGES.TARGET_ID, targetId);
        return this;
    }

    public Marriage setMarriedAt(long marriedAt) {
        set(MARRIAGES.MARRIED_AT, marriedAt);
        return this;
    }

    @NotNull
    public Marriage setLastUpdated(long updatedAt) {
        set(MARRIAGES.UPDATED_AT, updatedAt);
        return this;
    }
}