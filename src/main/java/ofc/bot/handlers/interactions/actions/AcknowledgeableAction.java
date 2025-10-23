package ofc.bot.handlers.interactions.actions;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import ofc.bot.handlers.interactions.commands.responses.InteractionResponseBuilder;
import ofc.bot.handlers.interactions.commands.responses.InteractionResponseData;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public interface AcknowledgeableAction<T, B> {

    /**
     * The id of the entity, usually an {@link Interaction}.
     *
     * @return The unique id of the entity.
     */
    long getId();

    @NotNull
    MessageChannel getChannel();

    long getChannelId();

    @NotNull
    Member getIssuer();

    @NotNull
    User getUser();

    long getUserId();

    @NotNull
    Guild getGuild();

    long getGuildId();

    @NotNull
    OffsetDateTime getTimeCreated();

    boolean isAcknowledged();

    @NotNull
    T getSource();

    /**
     * Acknowledges the interaction by sending a "{@code <Bot> is thinking...}".
     * <p>
     * This method has no effect if the interaction has been already acknowledged.
     *
     * @param ephemeral Whether this message should only be visible to the interaction user.
     */
    void ack(boolean ephemeral);

    /**
     * Acknowledges the interaction by sending a "{@code <Bot> is thinking...}".
     * <p>
     * This is non-ephemeral always. If you want to acknowledge an interaction
     * ephemerally, use {@link AcknowledgeableAction#ack(boolean) IAcknowledgeable.ack(true)} instead.
     * <p>
     * This method has no effect if the interaction has been already acknowledged.
     *
     * @see AcknowledgeableAction#ack(boolean)
     */
    default void ack() {
        ack(false);
    }

    @NotNull
    default InteractionResult replyFormat(@NotNull String format, @NotNull Object... args) {
        return reply(String.format(format, args));
    }

    /**
     * Creates a new response builder to this interaction.
     * <p>
     * If the final operation is a call to an {@code edit()} method,
     * the {@code ephemeral} argument will be ignored, as you cannot change this
     * property after the response is sent on most response types (usualy real Discord interactions).
     *
     * @param ephemeral whether the response should be an ephemeral message.
     * @return a new {@link InteractionResponseBuilder} instance to better structure the response.
     */
    @NotNull
    B create(boolean ephemeral);

    @NotNull
    default B create() {
        return create(false);
    }

    @NotNull
    InteractionResult reply(@NotNull String content, boolean ephemeral);

    @NotNull
    InteractionResult reply(@NotNull String content);

    @NotNull
    InteractionResult edit(@NotNull String content);

    @NotNull
    InteractionResult replyEmbeds(boolean ephemeral, @NotNull MessageEmbed... embeds);

    @NotNull
    InteractionResult replyEmbeds(InteractionResult result, boolean ephemeral, @NotNull MessageEmbed... embeds);

    @NotNull
    InteractionResult editEmbeds(@NotNull MessageEmbed... embeds);

    @NotNull
    default InteractionResult reply(InteractionResult res, boolean ephemeral) {
        reply(res.getContent(), ephemeral);
        return res;
    }

    @NotNull
    default InteractionResult reply(InteractionResult result) {
        reply(result.getContent(), result.isEphemeral());
        return result;
    }

    @NotNull
    default InteractionResult edit(InteractionResult res) {
        edit(res.getContent());
        return res;
    }

    @NotNull
    default InteractionResult replyEmbeds(@NotNull MessageEmbed... embeds) {
        return replyEmbeds(false, embeds);
    }

    @NotNull
    default InteractionResult replyEmbeds(InteractionResult result, @NotNull MessageEmbed... embeds) {
        return replyEmbeds(result, false, embeds);
    }

    @NotNull
    InteractionResult replyFiles(@NotNull FileUpload... files);

    @NotNull
    InteractionResult editFiles(@NotNull FileUpload... files);

    @NotNull
    default InteractionResult replyFile(@NotNull byte[] data, @NotNull String fileName) {
        return replyFiles(FileUpload.fromData(data, fileName));
    }

    @NotNull
    InteractionResult replyModal(@NotNull Modal modal);

    @NotNull
    InteractionResult edit(InteractionResponseData data);

    @NotNull
    InteractionResult reply(InteractionResponseData data);
}