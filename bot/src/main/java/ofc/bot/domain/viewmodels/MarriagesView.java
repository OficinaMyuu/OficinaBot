package ofc.bot.domain.viewmodels;

import java.util.List;

public record MarriagesView(
        List<MarriageView> marriages,
        long userId,
        int page,
        int maxPages,
        int marriageCount
) {
    public boolean isEmpty() {
        return this.marriages.isEmpty();
    }
}