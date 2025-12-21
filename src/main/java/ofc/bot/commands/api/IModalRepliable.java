package ofc.bot.commands.api;

import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

public interface IModalRepliable {
    void replyModal(@NotNull Modal modal);
}