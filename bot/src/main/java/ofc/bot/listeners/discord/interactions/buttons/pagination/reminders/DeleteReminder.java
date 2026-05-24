package ofc.bot.listeners.discord.interactions.buttons.pagination.reminders;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.Reminder;
import ofc.bot.domain.sqlite.repository.ReminderRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Reminders.DELETE_REMINDER, autoResponseType = AutoResponseType.DEFER_EDIT)
public class DeleteReminder implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteReminder.class);
    private final ReminderRepository remRepo;

    public DeleteReminder(ReminderRepository remRepo) {
        this.remRepo = remRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        User user = ctx.getUser();
        long userId = user.getIdLong();
        int pageIndex = ctx.get("page_index");
        int reminderId = ctx.get("reminder_id");
        int newPageIndex = Math.max(pageIndex - 1, 0);

        try {
            Reminder rem = remRepo.findActiveById(reminderId);
            if (rem == null) {
                return Status.REMINDER_NOT_FOUND;
            }
            rem.setExpired(true).tickUpdate();
            remRepo.upsert(rem);

            PageItem<Reminder> page = Paginator.viewReminders(userId, newPageIndex);

            if (page.isEmpty())
                return Status.YOU_DONT_HAVE_REMINDERS;

            Reminder reminder = page.get(0);
            List<Button> buttons = EntityContextFactory.createRemindersButtons(reminder, newPageIndex, page.hasMore());
            MessageEmbed embed = EmbedFactory.embedReminder(user, reminder);

            return ctx.create()
                    .setEmbeds(embed)
                    .setActionRows(buttons)
                    .edit();
        } catch (DataAccessException e) {
            LOGGER.error("Failed to set reminder {} as expired", reminderId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }
}