package ofc.bot.handlers.giveaway;

import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.GiveawayWinner;

import java.util.List;

public record GiveawayEndResult(
        Giveaway giveaway,
        int entryCount,
        List<GiveawayWinner> winners,
        boolean changed
) {}
