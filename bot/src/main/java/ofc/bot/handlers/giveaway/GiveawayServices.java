package ofc.bot.handlers.giveaway;

import ofc.bot.domain.sqlite.repository.Repositories;

public final class GiveawayServices {
    private GiveawayServices() {}

    public static GiveawayService create() {
        var giveawayRepo = Repositories.getGiveawayRepository();
        var entryRepo = Repositories.getGiveawayEntryRepository();
        var winnerRepo = Repositories.getGiveawayWinnerRepository();

        return new GiveawayService(
                giveawayRepo,
                entryRepo,
                winnerRepo,
                Repositories.getColorRoleItemRepository(),
                Repositories.getColorRoleStateRepository(),
                new GiveawayWinnerSelector(),
                new GiveawayMessageUpdater(giveawayRepo, entryRepo, winnerRepo)
        );
    }
}
