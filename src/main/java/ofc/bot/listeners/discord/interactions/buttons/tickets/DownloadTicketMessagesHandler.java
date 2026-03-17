package ofc.bot.listeners.discord.interactions.buttons.tickets;

import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.MessageVersion;
import ofc.bot.domain.entity.SupportTicket;
import ofc.bot.domain.sqlite.repository.MessageVersionRepository;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@InteractionHandler(scope = Scopes.Tickets.DOWNLOAD_MESSAGES, autoResponseType = AutoResponseType.THINKING)
public class DownloadTicketMessagesHandler implements InteractionListener<ButtonClickContext> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    private final MessageVersionRepository msgVrsRepo;
    private final UserRepository userRepo;

    public DownloadTicketMessagesHandler(MessageVersionRepository msgVrsRepo, UserRepository userRepo) {
        this.msgVrsRepo = msgVrsRepo;
        this.userRepo = userRepo;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        SupportTicket ticket = ctx.get("ticket");
        List<MessageVersion> versions = msgVrsRepo.findByChannelIdAsc(ticket.getChannelId());

        if (versions.isEmpty()) return Status.NO_MESSAGES_FOUND;

        Map<Long, MessageState> states = reconstructMessages(versions);

        assignSequenceNumbers(states);

        String transcript = generateTranscript(states);
        String fileName = String.format("ticket-%02d-transcript.diff", ticket.getId());
        byte[] fileData = transcript.getBytes(StandardCharsets.UTF_8);

        return ctx.replyFile(fileData, fileName);
    }

    private Map<Long, MessageState> reconstructMessages(List<MessageVersion> versions) {
        Map<Long, MessageState> states = new LinkedHashMap<>();

        for (MessageVersion v : versions) {
            states.compute(v.getMessageId(), (id, existing) -> {
                if (existing == null) {
                    return createInitialState(v);
                }
                return updateExistingState(existing, v);
            });
        }
        return states;
    }

    private MessageState createInitialState(MessageVersion v) {
        MessageState state = new MessageState();
        state.messageId = v.getMessageId();
        state.authorId = v.getAuthorId();
        state.refMessageId = v.getMessageReferenceId();
        state.content = v.getContent();
        state.createdAt = v.getTimeCreated();
        return state;
    }

    private MessageState updateExistingState(MessageState existing, MessageVersion v) {
        if (v.isDeleted()) {
            existing.isDeleted = true;
            return existing;
        }

        existing.content = v.getContent();
        existing.isEdited = true;
        return existing;
    }

    private void assignSequenceNumbers(Map<Long, MessageState> states) {
        int seq = 1;
        for (MessageState state : states.values()) {
            state.sequence = seq++;
        }
    }

    private String generateTranscript(Map<Long, MessageState> states) {
        StringBuilder sb = new StringBuilder();
        for (MessageState state : states.values()) {
            sb.append(formatMessage(state, states));
        }
        return sb.toString();
    }

    /**
     * Formats a single {@link MessageState} into a {@code .diff} compliant transcript string.
     * <p>
     * The generated string follows this general pattern:
     * <blockquote>
     * {@code [+/-] #[Seq] [[Time]] [#[RefSeq] <- ][Author]: [(edited) ][Content]}
     * </blockquote>
     * <p>
     * <b>Formatting Examples:</b>
     * <pre>{@code
     * // 1. Standard message
     * + #01 [14:30 15/08] JohnDoe: Hello, I need help with my account.
     * * // 2. Reply to sequence #01
     * + #02 [14:35 15/08] #1 <- SupportStaff: Hi John! What seems to be the issue?
     * * // 3. Edited message
     * + #03 [14:37 15/08] JohnDoe: (edited) I forgot my password and email.
     * * // 4. Deleted message (e.g., accidental reply)
     * - #04 [14:38 15/08] #2 <- JohnDoe: Wait, nevermind!
     * * // 5. Multi-line message (maintains diff structure across line breaks)
     * + #05 [14:40 15/08] SupportStaff: No problem! Here is what you can do:
     * + 1. Go to the website.
     * + 2. Click on "Recover Account".
     * }</pre>
     *
     * @param state     The current folded message state to format.
     * @param allStates A map of all folded states in the channel, used to resolve sequence numbers for replies.
     * @return The formatted {@code .diff} string block for this specific message.
     */
    private String formatMessage(MessageState state, Map<Long, MessageState> allStates) {
        AppUser author = userRepo.findById(state.authorId);
        String authorName = author != null ? author.getName() : "Unknown";
        String timestamp = TIME_FORMATTER.format(Instant.ofEpochSecond(state.createdAt));
        String replyPrefix = resolveReplyPrefix(state.refMessageId, allStates);

        char diffChar = state.isDeleted ? '-' : '+';
        String diffPrefix = diffChar + " ";
        String content = resolveContent(state);
        String[] lines = content.split("\n");

        StringBuilder formatted = new StringBuilder();

        // 1st Line formatting
        String firstLine = lines.length > 0 ? lines[0] : "";
        formatted.append(String.format("%c #%02d [%s] %s%s: %s\n",
                diffChar, state.sequence, timestamp, replyPrefix, authorName, firstLine));

        // Multi-line formatting
        for (int i = 1; i < lines.length; i++) {
            formatted.append(diffPrefix).append(lines[i]).append("\n");
        }

        return formatted.toString();
    }

    private String resolveContent(MessageState state) {
        String content = state.content == null ? "" : state.content;
        return state.isEdited ? "(edited) " + content : content;
    }

    private String resolveReplyPrefix(Long refMessageId, Map<Long, MessageState> allStates) {
        if (refMessageId == null) return "";

        MessageState refState = allStates.get(refMessageId);
        String sequenceTag = (refState != null) ? "#" + refState.sequence : "#?";
        return sequenceTag + " <- ";
    }

    private static class MessageState {
        long messageId;
        long authorId;
        Long refMessageId;
        String content;
        boolean isEdited;
        boolean isDeleted;
        long createdAt;
        int sequence;
    }
}
