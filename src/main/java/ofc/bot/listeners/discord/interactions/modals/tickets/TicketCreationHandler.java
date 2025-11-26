package ofc.bot.listeners.discord.interactions.modals.tickets;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.SupportTicketRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.modals.contexts.ModalSubmitContext;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.Channels;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static net.dv8tion.jda.api.Permission.*;

@InteractionHandler(scope = Scopes.Tickets.CREATE_TICKET, autoResponseType = AutoResponseType.THINKING_EPHEMERAL)
public class TicketCreationHandler implements InteractionListener<ModalSubmitContext> {
    public static final String CLOSE_BUTTON_ID = "close-ticket";
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketCreationHandler.class);
    private static final String CHANNEL_NAME_FORMAT = "%02d｜%s";
    private static final Button CLOSE_TICKET_BUTTON;
    private static final long ISSUER_ALLOWED_PERMS;
    private static final long ISSUER_BLOCKED_PERMS;
    private static final long STAFF_ALLOW_PERMS;
    private final SupportTicketRepository ticketRepo;

    public TicketCreationHandler(SupportTicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    @Override
    public InteractionResult onExecute(ModalSubmitContext ctx) {
        Category category = Channels.TICKETS.channel(Category.class);

        if (category == null)
            return Status.CHANNEL_CATEGORY_NOT_FOUND;

        Member member = ctx.getIssuer();
        String subject = ctx.getField("subject");
        String body = ctx.getField("body");
        MessageEmbed embed = EmbedFactory.embedCreatedTicket(member, subject, body);
        TextChannel channel = createChannel(category, member.getUser(), embed);
        long guildId = ctx.getGuildId();
        long userId = member.getIdLong();

        if (channel == null)
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;

        try {
            // Instantiating a new Ticket row on database
            long now = Bot.unixNow();
            long newChanId = channel.getIdLong();
            SupportTicket ticket = new SupportTicket(subject, body, guildId, newChanId, userId, null, null, now, now);
            ticketRepo.save(ticket);

            return Status.TICKET_OPENED_SUCCESSFULLY.args(channel.getAsMention());
        } catch (DataAccessException e) {
            LOGGER.error("Failed to save new ticket row to the database", e);
            // If the database fails to create a new row, then we also refuse to open the ticket altogether
            // I mean, if the bot also fails to delete the channel back, then we are not in a good day XD
            channel.delete().queue();
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    private TextChannel createChannel(Category parent, User user, MessageEmbed initialEmbed) {
        try {
            int id = ticketRepo.getLastId() + 1;
            long userId = user.getIdLong();
            String chanName = String.format(CHANNEL_NAME_FORMAT, id, user.getName());
            List<Role> staffRoles = getStaffRoles();
            ChannelAction<TextChannel> creation = parent.createTextChannel(chanName)
                    .addMemberPermissionOverride(userId, ISSUER_ALLOWED_PERMS, ISSUER_BLOCKED_PERMS);

            for (Role r : staffRoles) {
                creation.addRolePermissionOverride(r.getIdLong(), STAFF_ALLOW_PERMS, 0);
            }

            TextChannel chan = creation.complete();
            chan.sendMessageEmbeds(initialEmbed)
                    .addComponents(ActionRow.of(CLOSE_TICKET_BUTTON))
                    .queue();

            return chan;
        } catch (Exception e) {
            LOGGER.error("Could not create ticket channel at category {}", parent.getId(), e);
            return null;
        }
    }

    private List<Role> getStaffRoles() {
        return Staff.getByScope(Staff.Scope.SUPPORT)
                .stream()
                .filter(s -> s.getSeniority() > 0) // Only trainees are excluded
                .map(Staff::role)
                .filter(Objects::nonNull)
                .toList();
    }

    static {
        final Emoji lockEmoji = Emoji.fromUnicode("\uD83D\uDD12");
        ISSUER_ALLOWED_PERMS = Permission.getRaw(VIEW_CHANNEL, MESSAGE_ATTACH_FILES, MESSAGE_EMBED_LINKS);
        ISSUER_BLOCKED_PERMS = Permission.getRaw(MESSAGE_EXT_STICKER, MESSAGE_EXT_EMOJI, VOICE_USE_EXTERNAL_SOUNDS);

        STAFF_ALLOW_PERMS = Permission.getRaw(VIEW_CHANNEL, MESSAGE_MANAGE, MESSAGE_ATTACH_FILES);

        CLOSE_TICKET_BUTTON = Button.of(ButtonStyle.DANGER, CLOSE_BUTTON_ID, "Fechar Ticket", lockEmoji);
    }
}