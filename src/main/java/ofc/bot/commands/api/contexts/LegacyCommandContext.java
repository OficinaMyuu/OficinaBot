package ofc.bot.commands.api.contexts;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.commands.api.ICommandContext;
import ofc.bot.commands.api.builders.LegacyReplyAction;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LegacyCommandContext implements ICommandContext<Message> {
    private final Message msg;
    private final Member member; // Make IDE ignore nullable-checks

    public LegacyCommandContext(Message msg) {
        Checks.notNull(msg, "Message");
        this.msg = msg;
        this.member = msg.getMember();
    }

    @NotNull
    @Override
    public User getUser() {
        return this.msg.getAuthor();
    }

    @NotNull
    @Override
    public Member getMember() {
        return this.member;
    }

    @NotNull
    @Override
    public Guild getGuild() {
        return this.msg.getGuild();
    }

    @Override
    public GuildMessageChannelUnion getChannel() {
        return this.msg.getGuildChannel();
    }

    @NotNull
    @Override
    public Message getSource() {
        return this.msg;
    }

    @Override
    public void defer(boolean e) {
        this.getChannel()
                .sendTyping()
                .queue();
    }

    @Override
    public void reply(@NotNull String key, @NotNull DiscordLocale locale, @NotNull Map<String, Object> args) {

    }

    @Override
    public void replyRaw(@NotNull String message) {
        this.msg.reply(message).queue();
    }

    @Override
    public LegacyReplyAction replyRich() {
        return new LegacyReplyAction(this.msg);
    }
}