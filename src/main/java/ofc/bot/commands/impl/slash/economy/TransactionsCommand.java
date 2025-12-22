package ofc.bot.commands.impl.slash.economy;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.BankTransaction;
import ofc.bot.domain.entity.enums.TransactionType;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "transactions")
public class TransactionsCommand extends SlashCommand {
    public static final int PAGE_SIZE = 5;

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        int pageIndex = ctx.getOption("page", 1, OptionMapping::getAsInt) - 1;
        boolean crossEconomy = ctx.getOption("cross-economy", true, OptionMapping::getAsBoolean);
        Guild guild = ctx.getGuild();
        User user = ctx.getUser();
        List<TransactionType> actions = getTypes();
        List<CurrencyType> currencies = getCurrencies(crossEconomy);
        PageItem<BankTransaction> trs = Paginator.viewTransactions(userId, pageIndex, PAGE_SIZE, currencies, actions);

        if (trs.isEmpty())
            return Status.EMPTY_BANK_STATEMENT;

        if (!trs.exists(pageIndex))
            return Status.PAGE_DOES_NOT_EXIST.args(trs.getLastPage());

        MessageEmbed embed = EmbedFactory.embedTransactions(user, guild, trs);
        List<Button> btns = EntityContextFactory.createTransactionsButtons(
                userId, currencies, actions, pageIndex, trs.hasMore());

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(btns)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra o histórico de transações.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.BOOLEAN, "cross-economy", "Se devemos incluir meros registros de outras economias na lista (padrão: True)."),
                new OptionData(OptionType.INTEGER, "page", "A página do histórico de transações.")
                        .setRequiredRange(1, Integer.MAX_VALUE)
        );
    }

    private List<TransactionType> getTypes() {
        return TransactionType.allExcept();
    }

    private List<CurrencyType> getCurrencies(boolean crossEconomy) {
        return crossEconomy ? List.of(CurrencyType.values()) : List.of(CurrencyType.OFICINA);
    }
}