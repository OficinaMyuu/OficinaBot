package ofc.bot.handlers.interactions.actions.impl;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.handlers.interactions.actions.AcknowledgeableAction;
import ofc.bot.handlers.interactions.commands.responses.InteractionResponseBuilder;
import ofc.bot.handlers.interactions.commands.responses.InteractionResponseData;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public abstract class GenericInteractionRepliableAction<T extends Interaction & IReplyCallback>
        implements AcknowledgeableAction<T, InteractionResponseBuilder> {
    private final T itr;
    private final Member member;
    private final Guild guild;

    public GenericInteractionRepliableAction(T itr) {
        Member member = itr.getMember();
        Guild guild = itr.getGuild();
        Checks.notNull(itr, "Interaction");
        Checks.notNull(member, "Interaction member");
        Checks.notNull(guild, "Acknowledgeable interactions must be in a Guild");

        this.itr = itr;
        this.member = member;
        this.guild = guild;
    }

    @Override
    public long getId() {
        return this.itr.getIdLong();
    }

    @Override
    @NotNull
    public MessageChannel getChannel() {
        return this.itr.getMessageChannel();
    }

    @Override
    public long getChannelId() {
        return itr.getChannelIdLong();
    }

    @Override
    @NotNull
    public Member getIssuer() {
        return this.member;
    }

    @NotNull
    @Override
    public User getUser() {
        return this.itr.getUser();
    }

    @Override
    public long getUserId() {
        return getUser().getIdLong();
    }

    @Override
    @NotNull
    public Guild getGuild() {
        return this.guild;
    }

    @Override
    public long getGuildId() {
        return this.guild.getIdLong();
    }

    @Override
    @NotNull
    public OffsetDateTime getTimeCreated() {
        return this.itr.getTimeCreated();
    }

    @Override
    public boolean isAcknowledged() {
        return this.itr.isAcknowledged();
    }

    @Override
    @NotNull
    public T getSource() {
        return this.itr;
    }

    @Override
    public void ack(boolean ephemeral) {
        if (!isAcknowledged()) {
            this.itr.deferReply(ephemeral).queue();
        }
    }

    @Override
    @NotNull
    public InteractionResponseBuilder create(boolean ephemeral) {
        return new InteractionResponseBuilder(this).setEphemeral(ephemeral);
    }

    @Override
    @NotNull
    public InteractionResult reply(@NotNull String content, boolean ephemeral) {
        return this.create(ephemeral).setContent(content).send();
    }

    @Override
    @NotNull
    public InteractionResult reply(@NotNull String content) {
        return this.reply(content, false);
    }

    @Override
    @NotNull
    public InteractionResult edit(@NotNull String content) {
        return this.create().setContent(content).edit();
    }

    @Override
    @NotNull
    public InteractionResult replyEmbeds(boolean ephemeral, @NotNull MessageEmbed... embeds) {
        return this.replyEmbeds(Status.OK, ephemeral, embeds);
    }

    @Override
    @NotNull
    public InteractionResult replyEmbeds(InteractionResult result, boolean ephemeral, @NotNull MessageEmbed... embeds) {
        return this.create(ephemeral).setEmbeds(embeds).send(result);
    }

    @Override
    @NotNull
    public InteractionResult editEmbeds(@NotNull MessageEmbed... embeds) {
        return this.create().setEmbeds(embeds).edit();
    }

    @Override
    @NotNull
    public InteractionResult replyFiles(@NotNull FileUpload... files) {
        return this.create().setFiles(files).send();
    }

    @Override
    @NotNull
    public InteractionResult editFiles(@NotNull FileUpload... files) {
        return this.create().setFiles().send();
    }

    @Override
    @NotNull
    public InteractionResult replyModal(@NotNull Modal modal) {
        if (isAcknowledged())
            throw new UnsupportedOperationException("Cannot reply a Modal to an acknowledged interaction");

        if (!(itr instanceof IModalCallback modalItr))
            throw new UnsupportedOperationException("This interaction does not support Modals");

        modalItr.replyModal(modal).queue();
        return Status.OK;
    }

    @Override
    @NotNull
    public final InteractionResult edit(InteractionResponseData data) {
        this.itr.getHook()
                .editOriginal(data.toEditData())
                .queue(data.getSuccessHook(), data.getFailureHook());

        return Status.OK;
    }

    @Override
    @NotNull
    public final InteractionResult reply(InteractionResponseData data) {
        if (!isAcknowledged()) {
            this.itr.reply(data.toCreateData())
                    .setEphemeral(data.isEphemeral())
                    .queue(data.getSuccessSend(), data.getFailureSend());
        } else {
            this.itr.getHook()
                    .sendMessage(data.toCreateData())
                    .setEphemeral(data.isEphemeral())
                    .queue(data.getSuccessHook(), data.getFailureHook());
        }
        return Status.OK;
    }
}