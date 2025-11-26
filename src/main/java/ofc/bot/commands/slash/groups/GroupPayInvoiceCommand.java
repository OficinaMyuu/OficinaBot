package ofc.bot.commands.slash.groups;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.RentStatus;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "group pay-invoice")
public class GroupPayInvoiceCommand extends SlashSubcommand {
    private final OficinaGroupRepository grpRepo;

    public GroupPayInvoiceCommand(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        Member issuer = ctx.getIssuer();
        OficinaGroup group = grpRepo.findByOwnerId(userId);

        if (group == null)
            return Status.YOU_DO_NOT_OWN_A_GROUP;

        if (group.getRentStatus() == RentStatus.NOT_PAID)
            return Status.YOU_CAN_NO_LONGER_PAY_THIS_INVOICE;

        int invoice = (int) group.calcCurrentInvoice();
        if (invoice == 0 || group.getRentStatus() == RentStatus.PAID)
            return Status.NO_PENDING_INVOICES;

        Button confirm = EntityContextFactory.createInvoiceConfirm(group, invoice);
        MessageEmbed embed = EmbedFactory.embedInvoicePayment(issuer, group, invoice);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(confirm)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Pague a fatura do seu grupo (normalmente será apenas o aluguel).";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(2, TimeUnit.MINUTES);
    }
}