package ofc.bot.listeners.discord.interactions.buttons.pagination;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.entity.Birthday;
import ofc.bot.domain.sqlite.repository.BirthdayRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.time.Month;
import java.util.List;

@InteractionHandler(scope = Scopes.Misc.PAGINATE_BIRTHDAYS, autoResponseType = AutoResponseType.DEFER_EDIT)
public class BirthdayPageUpdate implements InteractionListener<ButtonClickContext> {
    private final BirthdayRepository bdayRepo;

    public BirthdayPageUpdate(BirthdayRepository bdayRepo) {
        this.bdayRepo = bdayRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        Month month = ctx.get("month");
        List<Birthday> birthdays = bdayRepo.findByMonth(month);
        List<Button> newButtons = EntityContextFactory.createBirthdayListButtons(month);
        MessageEmbed newEmbed = EmbedFactory.embedBirthdayList(birthdays, guild, month);

        return ctx.create()
                .setEmbeds(newEmbed)
                .setActionRows(newButtons)
                .edit();
    }
}