package ofc.bot.listeners.discord.interactions.buttons.pagination.reminders;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.Reminder;
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

import java.util.List;

@InteractionHandler(scope = Scopes.Reminders.VIEW_REMINDERS, autoResponseType = AutoResponseType.DEFER_EDIT)
public class RemindersPageUpdate implements InteractionListener<ButtonClickContext> {

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        User user = ctx.getUser();
        long userId = user.getIdLong();
        int pageIndex = ctx.get("page_index");
        PageItem<Reminder> page = Paginator.viewReminders(userId, pageIndex);

        if (page.isEmpty())
            return Status.PAGE_IS_EMPTY;

        Reminder reminder = page.get(0);
        List<Button> buttons = EntityContextFactory.createRemindersButtons(reminder, pageIndex, page.hasMore());
        MessageEmbed embed = EmbedFactory.embedReminder(user, reminder);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .edit();
    }
}