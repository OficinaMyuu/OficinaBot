package ofc.bot.commands.api.builders;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import ofc.bot.commands.api.IReplyAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LegacyReplyAction implements IReplyAction {
    private final MessageCreateAction action;

    public LegacyReplyAction(Message source) {
        this.action = source.reply("");
    }

    @NotNull
    @Override
    public IReplyAction setContent(@Nullable String content) {
        action.setContent(content);
        return this;
    }

    @NotNull
    @Override
    public IReplyAction setEmbeds(@NotNull List<MessageEmbed> embeds) {
        action.setEmbeds(embeds);
        return this;
    }

    @NotNull
    @Override
    public IReplyAction setEphemeral(boolean ephemeral) {
        // We cannot reply to messages ephemerally, so just ignore it.
        return this;
    }

    @Override
    public void queue() {
        this.action.queue();
    }
}