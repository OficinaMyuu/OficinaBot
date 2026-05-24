package ofc.bot.handlers.interactions.commands.responses;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public interface InteractionResponseData extends MessageData {

    @NotNull
    List<FileUpload> getAttachments();

    @NotNull
    MessageCreateData toCreateData();

    @NotNull
    MessageEditData toEditData();

    boolean isEphemeral();

    Consumer<Message> getSuccessHook();

    Consumer<InteractionHook> getSuccessSend();

    Consumer<Throwable> getFailureHook();

    Consumer<Throwable> getFailureSend();

    InteractionResult send(InteractionResult res);

    InteractionResult edit(InteractionResult res);

    default InteractionResult send(Status status, Object... args) {
        return send(status.args(args));
    }

    default InteractionResult send() {
        return send(Status.OK);
    }

    default InteractionResult edit(Status status, Object... args) {
        return edit(status.args(args));
    }

    default InteractionResult edit() {
        return edit(Status.OK);
    }
}