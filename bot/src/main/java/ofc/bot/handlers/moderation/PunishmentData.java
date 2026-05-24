package ofc.bot.handlers.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

public record PunishmentData(
        MessageChannel channel,
        Member target,
        Member author,
        Reason reason
) {
    public PunishmentData(
            @NotNull MessageChannel channel, @NotNull Member target,
            @NotNull Member author, @NotNull Reason reason
    ) {
        Checks.notNull(channel, "Channel");
        Checks.notNull(target, "Target");
        Checks.notNull(author, "Author");
        Checks.notNull(reason, "Reason");

        Checks.check(!reason.isEmpty(), "Reason cannot be empty");
        Checks.check(!target.hasPermission(Permission.ADMINISTRATOR),
                "Target is immune to punishments (ADMINISTRATOR)");

        this.target = target;
        this.author = author;
        this.reason = reason;
        this.channel = channel;
    }
}