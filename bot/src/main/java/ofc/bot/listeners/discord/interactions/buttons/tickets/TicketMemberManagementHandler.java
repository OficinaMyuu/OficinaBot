package ofc.bot.listeners.discord.interactions.buttons.tickets;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.SupportTicketRepository;
import ofc.bot.handlers.tickets.TicketMemberAction;
import ofc.bot.handlers.tickets.TicketMemberUpdatePolicy;
import ofc.bot.listeners.discord.interactions.modals.tickets.TicketCreationHandler;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@DiscordEventHandler
public class TicketMemberManagementHandler extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketMemberManagementHandler.class);
    private static final String ADD_SELECT_ID = "ticket-members:add-select";
    private static final String REMOVE_SELECT_ID = "ticket-members:remove-select";
    private static final int MAX_SELECTED_MEMBERS = 10;

    private final SupportTicketRepository ticketRepo;

    public TicketMemberManagementHandler(SupportTicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        TicketMemberAction action = actionFromButtonId(buttonId);

        if (action == null) return;
        if (!event.isFromGuild()) return;

        Member actor = event.getMember();
        TextChannel channel = event.getChannel().asTextChannel();
        SupportTicket ticket = ticketRepo.findByChannelId(channel.getIdLong());

        if (actor == null || ticket == null || ticket.isClosed()) {
            event.reply("> Ticket não encontrado ou já fechado.").setEphemeral(true).queue();
            return;
        }

        if (!canManageTicketMembers(actor, ticket)) {
            event.reply("> Você não pode gerenciar membros deste ticket.").setEphemeral(true).queue();
            return;
        }

        event.reply(menuIntro(action))
                .setEphemeral(true)
                .addComponents(ActionRow.of(createMemberSelect(action)))
                .queue();
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String componentId = event.getComponentId();
        TicketMemberAction action = actionFromSelectId(componentId);

        if (action == null) return;
        if (!event.isFromGuild()) return;

        Member actor = event.getMember();
        TextChannel channel = event.getChannel().asTextChannel();
        SupportTicket ticket = ticketRepo.findByChannelId(channel.getIdLong());

        if (actor == null || ticket == null || ticket.isClosed()) {
            event.reply("> Ticket não encontrado ou já fechado.").setEphemeral(true).queue();
            return;
        }

        if (!canManageTicketMembers(actor, ticket)) {
            event.reply("> Você não pode gerenciar membros deste ticket.").setEphemeral(true).queue();
            return;
        }

        List<Member> selectedMembers = selectedMembers(event);
        List<TicketMemberUpdatePolicy.Decision> decisions = new ArrayList<>(selectedMembers.size());

        for (Member target : selectedMembers) {
            TicketMemberUpdatePolicy.Candidate candidate = createCandidate(channel, target);
            TicketMemberUpdatePolicy.Decision decision = TicketMemberUpdatePolicy.decide(
                    action,
                    candidate,
                    ticket.getInitiatorId()
            );

            decisions.add(decision);

            if (!decision.shouldApply()) {
                continue;
            }

            applyMemberUpdate(channel, target, action);
        }

        TicketMemberUpdatePolicy.Summary summary = TicketMemberUpdatePolicy.summarize(action, decisions);
        event.reply(summary.toUserMessage()).setEphemeral(true).queue();
    }

    private EntitySelectMenu createMemberSelect(TicketMemberAction action) {
        return EntitySelectMenu.create(selectId(action), EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder(action == TicketMemberAction.ADD ? "Adicionar usuários" : "Remover usuários")
                .setRequiredRange(1, MAX_SELECTED_MEMBERS)
                .build();
    }

    private TicketMemberUpdatePolicy.Candidate createCandidate(TextChannel channel, Member member) {
        PermissionOverride override = channel.getPermissionOverride(member);
        boolean admin = member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR);

        return new TicketMemberUpdatePolicy.Candidate(
                member.getIdLong(),
                member.hasAccess(channel),
                override != null,
                admin
        );
    }

    private void applyMemberUpdate(TextChannel channel, Member target, TicketMemberAction action) {
        try {
            if (action == TicketMemberAction.ADD) {
                channel.getManager().putMemberPermissionOverride(
                        target.getIdLong(),
                        TicketCreationHandler.TICKET_ALLOWED_PERMS,
                        TicketCreationHandler.TICKET_BLOCKED_PERMS
                ).queue();
                return;
            }

            PermissionOverride override = channel.getPermissionOverride(target);
            if (override != null) {
                override.delete().queue();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update ticket member permissions in channel {}", channel.getId(), e);
        }
    }

    private boolean canManageTicketMembers(Member actor, SupportTicket ticket) {
        return actor.getIdLong() == ticket.getInitiatorId()
                || Staff.isStaff(actor)
                || actor.hasPermission(Permission.MANAGE_CHANNEL);
    }

    private List<Member> selectedMembers(EntitySelectInteractionEvent event) {
        return event.getMentions().getMembers();
    }

    private String menuIntro(TicketMemberAction action) {
        return action == TicketMemberAction.ADD
                ? "> Selecione quem deve entrar neste ticket."
                : "> Selecione quem deve sair deste ticket.";
    }

    private TicketMemberAction actionFromButtonId(String buttonId) {
        return switch (buttonId) {
            case TicketCreationHandler.ADD_MEMBERS_BUTTON_ID -> TicketMemberAction.ADD;
            case TicketCreationHandler.REMOVE_MEMBERS_BUTTON_ID -> TicketMemberAction.REMOVE;
            default -> null;
        };
    }

    private TicketMemberAction actionFromSelectId(String selectId) {
        return switch (selectId) {
            case ADD_SELECT_ID -> TicketMemberAction.ADD;
            case REMOVE_SELECT_ID -> TicketMemberAction.REMOVE;
            default -> null;
        };
    }

    private String selectId(TicketMemberAction action) {
        return action == TicketMemberAction.ADD ? ADD_SELECT_ID : REMOVE_SELECT_ID;
    }
}
