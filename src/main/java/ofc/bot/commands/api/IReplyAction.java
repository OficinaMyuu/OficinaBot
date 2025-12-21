package ofc.bot.commands.api;

import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IReplyAction {

    @NotNull
    IReplyAction setContent(@Nullable String content);

    @NotNull
    IReplyAction setEmbeds(@NotNull List<MessageEmbed> embeds);

    @NotNull
    default IReplyAction setEmbeds(@NotNull MessageEmbed... embeds) {
        return setEmbeds(List.of(embeds));
    }

    @NotNull
    IReplyAction setEphemeral(boolean ephemeral);

    void queue();
}