package ofc.bot.handlers.giveaway;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.Main;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.GiveawayWinner;
import ofc.bot.domain.database.repository.GiveawayEntryRepository;
import ofc.bot.domain.database.repository.GiveawayRepository;
import ofc.bot.domain.database.repository.GiveawayWinnerRepository;
import ofc.bot.handlers.ThrottledAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GiveawayMessageUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayMessageUpdater.class);
    private static final Duration UPDATE_INTERVAL = Duration.ofSeconds(1);

    private final GiveawayRepository giveawayRepo;
    private final GiveawayEntryRepository entryRepo;
    private final GiveawayWinnerRepository winnerRepo;
    private final Map<String, ThrottledAction<String>> updates = new ConcurrentHashMap<>();

    public GiveawayMessageUpdater(
            GiveawayRepository giveawayRepo,
            GiveawayEntryRepository entryRepo,
            GiveawayWinnerRepository winnerRepo
    ) {
        this.giveawayRepo = giveawayRepo;
        this.entryRepo = entryRepo;
        this.winnerRepo = winnerRepo;
    }

    public void post(String giveawayId) {
        updates.computeIfAbsent(giveawayId, id -> new ThrottledAction<>(UPDATE_INTERVAL, this::refreshNow))
                .post(giveawayId);
    }

    public void refreshNow(String giveawayId) {
        Giveaway giveaway = giveawayRepo.findById(giveawayId);
        if (giveaway == null) {
            shutdown(giveawayId);
            return;
        }

        JDA api = Main.getApi();
        TextChannel channel = api == null ? null : api.getTextChannelById(giveaway.getChannelId());
        if (channel == null) {
            return;
        }

        int entryCount = entryRepo.countByGiveaway(giveawayId);
        List<GiveawayWinner> winners = winnerRepo.findActiveByGiveaway(giveawayId);

        channel.retrieveMessageById(giveaway.getMessageId())
                .queue(message -> {
                    if (giveaway.isActive()) {
                        message.editMessageEmbeds(GiveawayMessageFactory.activeGiveaway(giveaway, entryCount))
                                .setComponents(ActionRow.of(
                                        GiveawayComponentFactory.joinButton(giveawayId),
                                        GiveawayComponentFactory.leaveButton(giveawayId)
                                ))
                                .queue();
                    } else {
                        message.editMessageEmbeds(GiveawayMessageFactory.endedGiveaway(giveaway, entryCount, winners))
                                .setComponents(GiveawayComponentFactory.requiresClaim(giveaway) && !winners.isEmpty()
                                        ? List.of(ActionRow.of(GiveawayComponentFactory.claimButton(giveawayId)))
                                        : List.of())
                                .queue();
                        shutdown(giveawayId);
                    }
                }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL));
    }

    public void shutdown(String giveawayId) {
        ThrottledAction<String> action = updates.remove(giveawayId);
        if (action != null) {
            action.shutdown();
        }
    }
}
