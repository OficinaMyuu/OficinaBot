package ofc.bot.commands.reminders;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ofc.bot.domain.entity.Reminder;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@DiscordCommand(name = "remind list")
public class ListRemindersCommand extends SlashSubcommand {

    @Override
    public InteractionResult onSlashCommand(SlashCommandContext ctx) {
        User user = ctx.getUser();
        int pageIndex = ctx.getOption("page", 1, OptionMapping::getAsInt) - 1;
        long userId = user.getIdLong();
        PageItem<Reminder> page = Paginator.viewReminders(userId, pageIndex);

        if (page.isEmpty())
            return Status.YOU_DONT_HAVE_REMINDERS;

        Reminder reminder = page.get(0);
        List<Button> buttons = EntityContextFactory.createRemindersButtons(reminder, pageIndex, page.hasMore());
        MessageEmbed embed = EmbedFactory.embedReminder(user, reminder);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRow(buttons)
                .send();
    }

    @Override
    protected void init() {
        setDesc("Lista todos os seus lembretes criados.");

        addOpt(OptionType.INTEGER, "page", "Qual página deve ser mostrada.", 1, Integer.MAX_VALUE);
    }
}