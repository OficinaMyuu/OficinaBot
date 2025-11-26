package ofc.bot.listeners.discord.interactions.buttons.pagination;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.commands.slash.economy.TransactionsCommand;
import ofc.bot.domain.entity.BankTransaction;
import ofc.bot.domain.entity.enums.TransactionType;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;

@InteractionHandler(scope = Scopes.Economy.VIEW_TRANSACTIONS, autoResponseType = AutoResponseType.DEFER_EDIT)
public class TransactionsPagination implements InteractionListener<ButtonClickContext> {

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        int pageIndex = ctx.get("page_index");
        long userId = ctx.get("user_id");
        List<CurrencyType> currencies = ctx.get("currencies");
        List<TransactionType> actions = ctx.get("actions");
        Guild guild = ctx.getGuild();
        PageItem<BankTransaction> trs = Paginator.viewTransactions(
                userId, pageIndex, TransactionsCommand.PAGE_SIZE, currencies, actions);

        if (trs.isEmpty())
            return Status.EMPTY_BANK_STATEMENT;

        Bot.fetchUser(userId).queue(user -> {
            MessageEmbed embed = EmbedFactory.embedTransactions(user, guild, trs);
            List<Button> btns = EntityContextFactory.createTransactionsButtons(
                    userId, currencies, actions, pageIndex, trs.hasMore());

            ctx.create()
                    .setEmbeds(embed)
                    .setActionRows(btns)
                    .edit();
        }, (err) -> ctx.reply(Status.USER_NOT_FOUND));
        return Status.OK;
    }
}