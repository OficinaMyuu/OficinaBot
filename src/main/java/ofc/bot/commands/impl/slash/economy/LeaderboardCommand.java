package ofc.bot.commands.impl.slash.economy;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.domain.viewmodels.LeaderboardUser;
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

@DiscordCommand(name = "leaderboard")
public class LeaderboardCommand extends SlashCommand {
    public static final int PAGE_SIZE = 10;
    private final UserEconomyRepository ecoRepo;

    public LeaderboardCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        int pageIndex = ctx.getOption("page", 1, OptionMapping::getAsInt) - 1;
        Scope scope = ctx.getEnumOption("filter", Scope.ALL, Scope.class);
        Guild guild = ctx.getGuild();
        Paginator<LeaderboardUser> paginator = Paginator.of((o) -> ecoRepo.viewLeaderboard(scope, o, PAGE_SIZE), ecoRepo::countAll, PAGE_SIZE);
        PageItem<LeaderboardUser> lb = paginator.next(pageIndex);
        int lastPageIndex = lb.lastPageIndex();

        if (pageIndex > lastPageIndex)
            return Status.PAGE_DOES_NOT_EXIST.args(lastPageIndex + 1);

        if (lb.isEmpty())
            return Status.LEADERBOARD_IS_EMPTY;

        boolean hasMorePages = lb.hasMore();
        List<Button> buttons = EntityContextFactory.createLeaderboardButtons(paginator, pageIndex, hasMorePages);
        MessageEmbed embed = EmbedFactory.embedLeaderboard(guild, lb);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Veja o placar de líderes global da economia.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "filter", "O filtro no tipo de saldo a ser mostrado.")
                        .addChoices(getChoices()),

                new OptionData(OptionType.INTEGER, "page", "A página do placar de líderes a verificar.")
                        .setRequiredRange(1, Integer.MAX_VALUE)
        );
    }

    private List<Command.Choice> getChoices() {
        return List.of(
                new Command.Choice(Scope.WALLET.getName(), Scope.WALLET.toString()),
                new Command.Choice(Scope.BANK.getName(), Scope.BANK.toString())
        );
    }

    public enum Scope {
        WALLET("Cash"),
        BANK("Bank"),
        ALL(null);

        private final String name;

        Scope(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}