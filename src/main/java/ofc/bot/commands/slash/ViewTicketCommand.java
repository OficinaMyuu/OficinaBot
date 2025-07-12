package ofc.bot.commands.slash;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ofc.bot.Main;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.DiscordMessageRepository;
import ofc.bot.domain.sqlite.repository.SupportTicketRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@DiscordCommand(name = "view-tickets", permissions = Permission.MANAGE_SERVER)
public class ViewTicketCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewTicketCommand.class);
    private final DiscordMessageRepository msgRepo;

    public ViewTicketCommand(DiscordMessageRepository msgRepo) {
        this.msgRepo = msgRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        JDA api = Main.getApi();
        SelfUser self = api.getSelfUser();
        Guild guild = ctx.getGuild();
        SupportTicketRepository.TicketStatus byStatus = ctx.getEnumOption("by-status", SupportTicketRepository.TicketStatus.class);
        User byUser = ctx.getOption("by-user", OptionMapping::getAsUser);
        PageItem<SupportTicket> tickets = Paginator.viewTickets(byUser, byStatus, 0);
        long selfId = self.getIdLong();

        if (tickets.isEmpty())
            return Status.NO_TICKETS_FOUND;

        SupportTicket ticket = tickets.get(0);
        long chanId = ticket.getChannelId();
        boolean hasMore = tickets.hasMore();
        Set<Long> users = msgRepo.findUsersByChannelId(chanId);
        users.remove(selfId);

        api.retrieveUserById(ticket.getInitiatorId()).queue(issuer -> {
            MessageEmbed embed = EmbedFactory.embedTicketPage(issuer, guild, ticket, users);
            List<Button> buttons = EntityContextFactory.createTicketsButtons(byUser, byStatus, 0, hasMore);

            ctx.create()
                    .setEmbeds(embed)
                    .setActionRow(buttons)
                    .send();
        }, (err) -> { // ????????
            LOGGER.error("We somehow did not find the user who issued the ticket for id #{}", ticket.getId(), err);
            ctx.reply(Status.USER_NOT_FOUND);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra informações sobre um ticket.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "by-user", "Filtra por usuário."),
                new OptionData(OptionType.STRING, "by-status", "O status de operação do ticket.")
                        .addChoices(getStatusChoices())
        );
    }

    private List<Command.Choice> getStatusChoices() {
        return Arrays.stream(SupportTicketRepository.TicketStatus.values())
                .map(ts -> new Command.Choice(ts.getDisplay(), ts.name()))
                .toList();
    }
}