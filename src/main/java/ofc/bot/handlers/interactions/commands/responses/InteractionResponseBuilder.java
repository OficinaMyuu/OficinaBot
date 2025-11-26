package ofc.bot.handlers.interactions.commands.responses;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.interactions.actions.AcknowledgeableAction;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class InteractionResponseBuilder implements InteractionResponseData {
    private static final Consumer<Throwable> DEFAULT_ERROR_HANDLER = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);
    private final AcknowledgeableAction<?, ?> acknowledgeable;
    private final MessageCreateBuilder builder;
    private Consumer<InteractionHook> successSend;
    private Consumer<Message> successHook;
    private Consumer<Throwable> failureSend = DEFAULT_ERROR_HANDLER;
    private Consumer<Throwable> failureHook = DEFAULT_ERROR_HANDLER;
    private boolean ephemeral;

    public InteractionResponseBuilder(AcknowledgeableAction<?, ?> acknowledgeable) {
        super();
        Checks.notNull(acknowledgeable, "Acknowledgeable Interaction");

        this.builder = new MessageCreateBuilder();
        this.ephemeral = false;
        this.acknowledgeable = acknowledgeable;
    }

    public InteractionResponseBuilder onHook(Consumer<Message> success, Consumer<Throwable> failure) {
        this.successHook = success;
        this.failureHook = failure == null ? DEFAULT_ERROR_HANDLER : failure;
        return this;
    }

    public InteractionResponseBuilder onHook(Consumer<Message> success) {
        this.successHook = success;
        return this;
    }

    public InteractionResponseBuilder onSend(Consumer<InteractionHook> success, Consumer<Throwable> failure) {
        this.successSend = success;
        this.failureSend = failure == null ? DEFAULT_ERROR_HANDLER : failure;
        return this;
    }

    public InteractionResponseBuilder onSend(Consumer<InteractionHook> success) {
        this.successSend = success;
        return this;
    }

    public InteractionResponseBuilder setEphemeral(boolean flag) {
        this.ephemeral = flag;
        return this;
    }

    public InteractionResponseBuilder setContent(String content) {
        builder.setContent(content);
        return this;
    }

    public InteractionResponseBuilder setContent(InteractionResult res) {
        return setContent(res.getContent());
    }

    public InteractionResponseBuilder setContentFormat(String format, Object... args) {
        return setContent(String.format(format, args));
    }

    public InteractionResponseBuilder setEmbeds(MessageEmbed... embeds) {
        builder.setEmbeds(embeds);
        return this;
    }

    public InteractionResponseBuilder setUsingComponentsV2(boolean flag) {
        builder.useComponentsV2(flag);
        return this;
    }

    public InteractionResponseBuilder setAllowedMentions(Collection<Message.MentionType> mentions) {
        builder.setAllowedMentions(mentions);
        return this;
    }

    public InteractionResponseBuilder noMentions() {
        return setAllowedMentions(List.of());
    }

    public InteractionResponseBuilder mentionUsers(long... ids) {
        builder.mentionUsers(ids);
        return this;
    }

    public InteractionResponseBuilder mentionRoles(long... ids) {
        builder.mentionRoles(ids);
        return this;
    }

    public InteractionResponseBuilder setFiles(FileUpload... files) {
        builder.setFiles(files);
        return this;
    }

    public InteractionResponseBuilder setComponents(MessageTopLevelComponent... components) {
        builder.setComponents(components);
        return this;
    }

    public InteractionResponseBuilder setComponents(List<? extends MessageTopLevelComponent> components) {
        builder.setComponents(components);
        return this;
    }

    public InteractionResponseBuilder setActionRows(ActionRowChildComponent... rows) {
        builder.setComponents(ActionRow.of(List.of(rows)));
        return this;
    }

    public InteractionResponseBuilder setActionRows(List<? extends ActionRowChildComponent> row) {
        builder.setComponents(ActionRow.of(row));
        return this;
    }

    @Override
    public InteractionResult send(InteractionResult res) {
        acknowledgeable.reply(this);
        return res;
    }

    @Override
    public InteractionResult edit(InteractionResult res) {
        return acknowledgeable.edit(this);
    }

    @Override
    public boolean isEphemeral() {
        return this.ephemeral;
    }

    @Override
    public Consumer<Message> getSuccessHook() {
        return this.successHook;
    }

    @Override
    public Consumer<InteractionHook> getSuccessSend() {
        return this.successSend;
    }

    @Override
    public Consumer<Throwable> getFailureHook() {
        return this.failureHook;
    }

    @Override
    public Consumer<Throwable> getFailureSend() {
        return this.failureSend;
    }

    @NotNull
    @Override
    public String getContent() {
        return builder.getContent();
    }

    @NotNull
    @Override
    public List<MessageEmbed> getEmbeds() {
        return builder.getEmbeds();
    }

    @NotNull
    @Override
    public List<MessageTopLevelComponentUnion> getComponents() {
        return builder.getComponents();
    }

    @Override
    public boolean isUsingComponentsV2() {
        return builder.isUsingComponentsV2();
    }

    @NotNull
    @Override
    public List<FileUpload> getAttachments() {
        return builder.getAttachments();
    }

    @NotNull
    @Override
    public MessageCreateData toCreateData() {
        return builder.build();
    }

    @NotNull
    @Override
    public MessageEditData toEditData() {
        return MessageEditData.fromCreateData(toCreateData());
    }

    @Override
    public boolean isSuppressEmbeds() {
        return builder.isSuppressEmbeds();
    }

    @NotNull
    @Override
    public Set<String> getMentionedUsers() {
        return builder.getMentionedUsers();
    }

    @NotNull
    @Override
    public Set<String> getMentionedRoles() {
        return builder.getMentionedRoles();
    }

    @NotNull
    @Override
    public EnumSet<Message.MentionType> getAllowedMentions() {
        return builder.getAllowedMentions();
    }

    @Override
    public boolean isMentionRepliedUser() {
        return builder.isMentionRepliedUser();
    }
}