package ofc.bot.handlers.paginations;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.entity.MemberPunishment;
import ofc.bot.domain.entity.Reminder;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.domain.viewmodels.LevelView;
import ofc.bot.util.Bot;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Paginator<T> {
    private final Function<Integer, List<T>> supplier;
    private final Supplier<Integer> counter;
    private final int limit;

    private Paginator(Function<Integer, List<T>> supplier, Supplier<Integer> counter, int limit) {
        Checks.notNull(supplier, "Supplier");
        Checks.notNull(counter, "Counter");
        Checks.positive(limit, "Limit");

        this.supplier = supplier;
        this.counter = counter;
        this.limit = limit;
    }

    public static <T> Paginator<T> of(Function<Integer, List<T>> supplier, Supplier<Integer> counter, int limit) {
        return new Paginator<>(supplier, counter, limit);
    }

    public static PageItem<MemberPunishment> viewInfractions(long targetId, long guildId,
                                                             int pageSize, int pageIndex, boolean showInactive) {
        final MemberPunishmentRepository pnshRepo = Repositories.getMemberPunishmentRepository();
        int offset = pageIndex * pageSize;
        int rowCount = pnshRepo.countByUserAndGuildId(targetId, guildId);
        int maxPages = Bot.calcMaxPages(rowCount, pageSize);
        List<MemberPunishment> punishments = pnshRepo.findByUserAndGuildId(
                targetId, guildId, pageSize, offset, showInactive);

        return new PageItem<>(punishments, pageIndex, offset, maxPages, rowCount);
    }

    public static PageItem<SupportTicket> viewTickets(User byUser, int pageIndex) {
        final SupportTicketRepository ticketRepo = Repositories.getSupportTicketRepository();

        // In this case, we don't need to calculate the offset, as only 1 result
        // can be shown per page, the offset is already the pageIndex itself.
        List<SupportTicket> tickets = ticketRepo.searchByUser(byUser, pageIndex, 1);
        int count = ticketRepo.countByUser(byUser);

        return new PageItem<>(tickets, pageIndex, pageIndex, count, count);
    }

    public static PageItem<Reminder> viewReminders(long userId, int pageIndex) {
        final ReminderRepository remRepo = Repositories.getReminderRepository();

        // In this case, we don't need to calculate the offset, as only 1 result
        // can be shown per page, the offset is already the pageIndex itself.
        List<Reminder> rems = remRepo.viewReminderByUserId(userId, pageIndex, 1);
        int count = remRepo.countByUserId(userId);

        return new PageItem<>(rems, pageIndex, pageIndex, count, count);
    }

    public static PageItem<LevelView> viewLevels(int pageSize, int pageIndex) {
        final UserXPRepository xpRepo = Repositories.getUserXPRepository();
        int offset = pageIndex * pageSize;
        int rowCount = xpRepo.countAll();
        int maxPages = Bot.calcMaxPages(rowCount, pageSize);
        List<LevelView> levels = xpRepo.viewLevels(offset, pageSize);

        return new PageItem<>(
                levels,
                pageIndex,
                offset,
                maxPages,
                rowCount
        );
    }

    public Function<Integer, List<T>> getSupplier() {
        return this.supplier;
    }

    public Supplier<Integer> getCounter() {
        return this.counter;
    }

    public int getLimit() {
        return this.limit;
    }

    /**
     * Attempts to fetch the first results produced by the supplied query.
     * <p>
     * This method is just a shortcut for {@link #next(int)} with {@code 0} as the {@code pageIndex}
     * parameter.
     *
     * @return A {@link PageItem} wrapping the produced results of the fetch operation.
     */
    public PageItem<T> start() {
        return next(0);
    }

    /**
     * Attempts to fetch the next rows in the database based on the provided page index.
     * <p>
     * The page index is, then, resolved into an {@code offset}, as {@code pageIndex * getLimit()}
     * and passed to the {@linkplain #getSupplier() supplier}.
     *
     * @param pageIndex The page index to fetch the results (starts at {@code 0}).
     * @return A {@link PageItem} wrapping the produced results of the fetch operation.
     */
    public PageItem<T> next(int pageIndex) {
        int offset = pageIndex * this.limit;
        int count = this.counter.get();
        int maxPages = Bot.calcMaxPages(count, this.limit);
        List<T> values = this.supplier.apply(offset);

        return new PageItem<>(values, pageIndex, offset, maxPages, count);
    }
}