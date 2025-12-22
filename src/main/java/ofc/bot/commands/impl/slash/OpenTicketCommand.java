package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.modals.Modal;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "ticket")
public class OpenTicketCommand extends SlashCommand {
    private static final int MAX_CHANNELS = 500;

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        Member issuer = ctx.getIssuer();
        int channelCount = guild.getChannels().size();

        if (channelCount >= MAX_CHANNELS)
            return Status.TICKETS_CANNOT_BE_CREATED_AT_THE_MOMENT;

        if (Staff.isStaff(issuer) && !issuer.hasPermission(Permission.ADMINISTRATOR))
            return Status.STAFF_CANNOT_CREATE_TICKETS;

        Modal modal = EntityContextFactory.createTicketModal();
        return ctx.replyModal(modal);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Abre um novo ticket.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(30, TimeUnit.MINUTES);
    }
}