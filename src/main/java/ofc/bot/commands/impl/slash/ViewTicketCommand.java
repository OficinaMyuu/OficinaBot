package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.Main;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
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

import java.util.List;
import java.util.Set;

@DiscordCommand(name = "view-tickets", permissions = Permission.MANAGE_SERVER)
public class ViewTicketCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ViewTicketCommand.class);
    private final MessageVersionRepository msgVrsRepo;

    public ViewTicketCommand(MessageVersionRepository msgVrsRepo) {
        this.msgVrsRepo = msgVrsRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        JDA api = Main.getApi();
        SelfUser self = api.getSelfUser();
        Guild guild = ctx.getGuild();
        User byUser = ctx.getOption("by-user", OptionMapping::getAsUser);
        PageItem<SupportTicket> tickets = Paginator.viewTickets(byUser, 0);
        long selfId = self.getIdLong();

        if (tickets.isEmpty())
            return Status.NO_TICKETS_FOUND;

        SupportTicket ticket = tickets.get(0);
        long chanId = ticket.getChannelId();
        boolean hasMore = tickets.hasMore();
        Set<Long> users = msgVrsRepo.findUsersByChannelId(chanId);
        users.remove(selfId);

        api.retrieveUserById(ticket.getInitiatorId()).queue(issuer -> {
            MessageEmbed embed = EmbedFactory.embedTicketPage(issuer, guild, ticket, users);
            List<Button> buttons = EntityContextFactory.createTicketsButtons(byUser, 0, hasMore);

            ctx.create()
                    .setEmbeds(embed)
                    .setActionRows(buttons)
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
                new OptionData(OptionType.USER, "by-user", "Filtra por usuário.")
        );
    }
}