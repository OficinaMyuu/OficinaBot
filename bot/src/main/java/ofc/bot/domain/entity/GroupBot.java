package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.BotCategory;
import ofc.bot.domain.tables.GroupBotsTable;

public class GroupBot extends OficinaRecord<GroupBot> {
    private static final GroupBotsTable GROUP_BOTS = GroupBotsTable.GROUP_BOTS;

    public GroupBot() {
        super(GROUP_BOTS);
    }

    public int getId() {
        return getValue(GROUP_BOTS.ID);
    }

    public long getBotId() {
        return get(GROUP_BOTS.BOT_ID);
    }

    public String getBotMention() {
        return "<@" + getBotId() + '>';
    }

    public String getBotName() {
        return get(GROUP_BOTS.BOT_NAME);
    }

    public BotCategory getBotCategory() {
        String cat = get(GROUP_BOTS.BOT_CATEGORY);
        return BotCategory.valueOf(cat);
    }

    public long getTimeCreated() {
        return get(GROUP_BOTS.CREATED_AT);
    }
}