package ofc.bot.domain.entity;

import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.tables.GroupsPerksTable;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.util.Bot;

public class GroupPerk extends OficinaRecord<GroupPerk> {
    private static final GroupsPerksTable GROUPS_PERKS = GroupsPerksTable.GROUPS_PERKS;

    public GroupPerk() {
        super(GROUPS_PERKS);
    }

    public GroupPerk(int groupId, long userId, StoreItemType item, int valuePaid, CurrencyType currency, long createdAt) {
        this();
        Checks.notNull(item, "Item");
        Checks.notNull(currency, "Currency");
        Checks.notNegative(valuePaid, "Value Paid");
        Checks.check(item.isGroup(), "Item %s is not group-scoped", item.name());

        set(GROUPS_PERKS.GROUP_ID, groupId);
        set(GROUPS_PERKS.USER_ID, userId);
        set(GROUPS_PERKS.ITEM, item.name());
        set(GROUPS_PERKS.VALUE_PAID, valuePaid);
        set(GROUPS_PERKS.CURRENCY, currency.name());
        set(GROUPS_PERKS.CREATED_AT, createdAt);
    }

    public GroupPerk(int groupId, long userId, StoreItemType item, int valuePaid, CurrencyType currency) {
        this(groupId, userId, item, valuePaid, currency, Bot.unixNow());
    }

    public GroupPerk(OficinaGroup group, long userId, StoreItemType item, int valuePaid, CurrencyType currency, long createdAt) {
        this(group.getId(), userId, item, valuePaid, currency, createdAt);
    }

    public GroupPerk(OficinaGroup group, long userId, StoreItemType item, int valuePaid, CurrencyType currency) {
        this(group.getId(), userId, item, valuePaid, currency);
    }

    public GroupPerk(OficinaGroup group, StoreItemType item, int valuePaid, CurrencyType currency) {
        this(group, group.getOwnerId(), item, valuePaid, currency);
    }

    public int getId() {
        return get(GROUPS_PERKS.ID);
    }

    public int getGroupId() {
        return get(GROUPS_PERKS.GROUP_ID);
    }

    public long getUserId() {
        return get(GROUPS_PERKS.USER_ID);
    }

    public StoreItemType getItem() {
        String item = get(GROUPS_PERKS.ITEM);
        return StoreItemType.valueOf(item);
    }

    public int getValuePaid() {
        return get(GROUPS_PERKS.VALUE_PAID);
    }

    public CurrencyType getCurrency() {
        String currency = get(GROUPS_PERKS.CURRENCY);
        return CurrencyType.valueOf(currency);
    }

    public long getTimeCreated() {
        return get(GROUPS_PERKS.CREATED_AT);
    }
}