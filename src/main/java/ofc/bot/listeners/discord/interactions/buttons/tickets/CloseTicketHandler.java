package ofc.bot.listeners.discord.interactions.buttons.tickets;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.Modal;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.listeners.discord.interactions.modals.tickets.TicketCreationHandler;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

@DiscordEventHandler
public class CloseTicketHandler extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        String buttonId = e.getComponentId();
        Member member = e.getMember();

        if (member == null || !buttonId.equals(TicketCreationHandler.CLOSE_BUTTON_ID)) return;

        if (!Staff.isStaff(member)) {
            e.reply("> ❌ Só staffs podem fechar um ticket... 😕").setEphemeral(true).queue();
            return;
        }

        Modal modal = EntityContextFactory.createTicketCloseModal();
        e.replyModal(modal).queue();
    }
}