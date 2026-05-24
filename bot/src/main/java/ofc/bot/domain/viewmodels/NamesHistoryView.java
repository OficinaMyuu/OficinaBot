package ofc.bot.domain.viewmodels;

import ofc.bot.domain.entity.UserNameUpdate;
import ofc.bot.util.Bot;

import java.util.List;

public record NamesHistoryView(
        List<UserNameUpdate> names,
        int total,
        int offset,
        int page
) {
    public boolean isEmpty() {
        return this.names.isEmpty();
    }

    public int maxPages() {
        return Bot.calcMaxPages(total, 10);
    }
}