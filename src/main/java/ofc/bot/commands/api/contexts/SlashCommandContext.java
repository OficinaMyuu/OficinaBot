package ofc.bot.commands.api.contexts;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildMessageChannelUnion;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.commands.api.ICommandContext;
import ofc.bot.commands.api.IModalRepliable;
import ofc.bot.commands.api.builders.SlashReplyAction;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SlashCommandContext implements ICommandContext<SlashCommandInteraction>, IModalRepliable {
    private final SlashCommandInteraction itr;
    private final Member member;

    public SlashCommandContext(SlashCommandInteraction itr) {
        Checks.notNull(itr, "Interaction");
        this.itr = itr;
        this.member = itr.getMember();
    }

    @NotNull
    @Override
    public User getUser() {
        return itr.getUser();
    }

    @NotNull
    @Override
    public Member getMember() {
        return this.member;
    }

    @NotNull
    @Override
    public Guild getGuild() {
        return this.member.getGuild();
    }

    @Override
    public GuildMessageChannelUnion getChannel() {
        return this.itr.getGuildChannel();
    }

    @NotNull
    @Override
    public SlashCommandInteraction getSource() {
        return this.itr;
    }

    @Override
    public void defer(boolean ephemeral) {

    }

    @Override
    public void reply(@NotNull String key, @NotNull DiscordLocale locale, @NotNull Map<String, Object> args) {

    }

    @Override
    public void replyRaw(@NotNull String message) {

    }

    @Override
    public SlashReplyAction replyRich() {
        return new SlashReplyAction(this.itr);
    }

    @Override
    public void replyModal(@NotNull Modal modal) {
        this.itr.replyModal(modal).queue();
    }
}