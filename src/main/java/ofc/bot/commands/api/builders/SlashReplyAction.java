package ofc.bot.commands.api.builders;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import ofc.bot.commands.api.IReplyAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SlashReplyAction implements IReplyAction {
    private final ReplyCallbackAction reply;

    public SlashReplyAction(SlashCommandInteraction source) {
        this.reply = source.reply("");
    }

    @NotNull
    @Override
    public IReplyAction setContent(@Nullable String content) {
        this.reply.setContent(content);
        return this;
    }

    @NotNull
    @Override
    public IReplyAction setEmbeds(@NotNull List<MessageEmbed> embeds) {
        this.reply.setEmbeds(embeds);
        return this;
    }

    @NotNull
    @Override
    public IReplyAction setEphemeral(boolean ephemeral) {
        this.reply.setEphemeral(ephemeral);
        return this;
    }

    @Override
    public void queue() {
        this.reply.queue();
    }
}