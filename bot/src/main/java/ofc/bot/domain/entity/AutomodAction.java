package ofc.bot.domain.entity;

import ofc.bot.domain.tables.AutomodActionsTable;
import ofc.bot.domain.entity.enums.PunishmentType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class AutomodAction extends OficinaRecord<AutomodAction> {
    private static final AutomodActionsTable AUTOMOD_ACTIONS = AutomodActionsTable.AUTOMOD_ACTIONS;

    public AutomodAction() {
        super(AUTOMOD_ACTIONS);
    }

    public int getId() {
        return get(AUTOMOD_ACTIONS.ID);
    }

    public int getThreshold() {
        return get(AUTOMOD_ACTIONS.THRESHOLD);
    }

    public int getDurationRaw() {
        return get(AUTOMOD_ACTIONS.DURATION);
    }

    public Duration getDuration() {
        return Duration.ofSeconds(getDurationRaw());
    }

    public PunishmentType getAction() {
        String action = get(AUTOMOD_ACTIONS.ACTION);
        return PunishmentType.valueOf(action);
    }

    public long getTimeCreated() {
        return get(AUTOMOD_ACTIONS.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(AUTOMOD_ACTIONS.UPDATED_AT);
    }

    public AutomodAction setThreshold(int threshold) {
        set(AUTOMOD_ACTIONS.THRESHOLD, threshold);
        return this;
    }

    public AutomodAction setDuration(int duration) {
        set(AUTOMOD_ACTIONS.DURATION, duration);
        return this;
    }

    public AutomodAction setAction(PunishmentType action) {
        set(AUTOMOD_ACTIONS.ACTION, action.name());
        return this;
    }

    @NotNull
    public AutomodAction setLastUpdated(long updatedAt) {
        set(AUTOMOD_ACTIONS.UPDATED_AT, updatedAt);
        return this;
    }
}