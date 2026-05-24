package ofc.bot.commands.impl.slash.tickets;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.SupportTicketRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.listeners.discord.interactions.modals.tickets.TicketCreationHandler;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "tickets merge")
public class MergeTicketCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergeTicketCommand.class);
    private static final String BELL_EMOJI = "\uD83D\uDD14";
    private static final long TICKET_ALLOWED_PERMS = TicketCreationHandler.TICKET_ALLOWED_PERMS;
    private static final long TICKET_BLOCKED_PERMS = TicketCreationHandler.TICKET_BLOCKED_PERMS;

    private final SupportTicketRepository ticketRepo;

    public MergeTicketCommand(SupportTicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        User actor = ctx.getUser();
        int dupId = ctx.getSafeOption("duplicate", OptionMapping::getAsInt);
        int destId = ctx.getSafeOption("destination", OptionMapping::getAsInt);

        if (dupId == destId)  return Status.CANNOT_MERGE_SAME_TICKET;

        SupportTicket duplicate = ticketRepo.findById(dupId);
        SupportTicket destination = ticketRepo.findById(destId);

        if (duplicate == null || destination == null) return Status.TICKET_NOT_FOUND;

        if (duplicate.isClosed() || destination.isClosed()) return Status.TICKET_ALREADY_CLOSED;

        TextChannel dupChan = guild.getTextChannelById(duplicate.getChannelId());
        TextChannel destChan = guild.getTextChannelById(destination.getChannelId());

        if (destChan == null) return Status.DESTINATION_CHANNEL_NOT_FOUND;

        long initiatorId = duplicate.getInitiatorId();
        destChan.getManager().putMemberPermissionOverride(
                initiatorId,
                TICKET_ALLOWED_PERMS,
                TICKET_BLOCKED_PERMS
        ).queue();

        sendMergeAlert(destChan, actor, dupId);

        if (dupChan != null) {
            dupChan.delete()
                    .reason("Merged into ticket #" + destId)
                    .queue();
        }

        duplicate.setClosedAuthorId(actor.getIdLong())
                .setCloseReason("Mesclado ao Ticket #" + destId)
                .setMergedInto(destId)
                .tickUpdate();

        try {
            ticketRepo.upsert(duplicate);
            return Status.TICKET_MERGED_SUCCESSFULLY;
        } catch (DataAccessException e) {
            LOGGER.error("Failed to upsert close/merge information to the database", e);
            // While its not good to return a full error here, since the merging
            // might have worked and the only issue was our database upsert call,
            // there is not much I am going to do here, sorry.
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mescla um ticket duplicado em outro ticket aberto.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "duplicate", "O ticket que será fechado/apagado.", true, true),
                new OptionData(OptionType.INTEGER, "destination", "O ticket que permanecerá aberto.", true, true)
        );
    }

    private void sendMergeAlert(MessageChannel chan, User actor, int duplicate) {
        chan.sendMessageFormat("> %s **Notificação:** O ticket **#%02d** foi mesclado neste canal por %s." +
                " O autor original foi adicionado a este canal.",
                BELL_EMOJI, duplicate, actor.getAsMention()
        ).queue();
    }

    @DiscordEventHandler
    public static class TicketMergeAutocompletionHandler extends ListenerAdapter {
        private final SupportTicketRepository ticketRepo;

        public TicketMergeAutocompletionHandler(SupportTicketRepository ticketRepo) {
            this.ticketRepo = ticketRepo;
        }

        @Override
        public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
            if (!e.getFullCommandName().equals("tickets merge")) return;

            String focusName = e.getFocusedOption().getName();
            String search = e.getFocusedOption().getValue().strip();

            if (focusName.equals("duplicate")) {
                handleDuplicateAutocompletion(e, search);
            } else if (focusName.equals("destination")) {
                handleDestinationAutocompletion(e, search);
            }
        }

        private void handleDuplicateAutocompletion(CommandAutoCompleteInteractionEvent e, String search) {
            List<SupportTicket> openTickets = ticketRepo.searchOpenTickets(search);
            List<Command.Choice> choices = toChoices(openTickets, null);

            e.replyChoices(choices).queue();
        }

        private void handleDestinationAutocompletion(CommandAutoCompleteInteractionEvent e, String search) {
            List<SupportTicket> openTickets = ticketRepo.searchOpenTickets(search);
            Integer dupId = null;

            try {
                dupId = e.getOption("duplicate", OptionMapping::getAsInt);
            } catch (Exception ignored) {}

            List<Command.Choice> choices = toChoices(openTickets, dupId);
            e.replyChoices(choices).queue();
        }

        private List<Command.Choice> toChoices(List<SupportTicket> tickets, Integer excludeId) {
            return tickets.stream()
                    .filter(t -> excludeId == null || t.getId() != excludeId)
                    .map(t -> {
                        String label = String.format("#%02d | %s", t.getId(), t.getTitle());
                        return new Command.Choice(Bot.limitStr(label, 100), t.getId());
                    })
                    .limit(OptionData.MAX_CHOICES)
                    .toList();
        }
    }
}