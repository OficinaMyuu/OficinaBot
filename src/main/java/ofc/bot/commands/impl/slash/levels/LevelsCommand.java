package ofc.bot.commands.impl.slash.levels;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.sqlite.repository.UserXPRepository;
import ofc.bot.domain.viewmodels.LevelView;
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

@DiscordCommand(name = "levels")
public class LevelsCommand extends SlashCommand {
    public static final int PAGE_SIZE = 10;
    private final UserXPRepository xpRepo;

    public LevelsCommand(UserXPRepository xpRepo) {
        this.xpRepo = xpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        int pageIndex = ctx.getOption("page", 1, OptionMapping::getAsInt) - 1;
        long userId = ctx.getUserId();
        Guild guild = ctx.getGuild();
        PageItem<LevelView> pageItem = Paginator.viewLevels(PAGE_SIZE, pageIndex);

        if (!pageItem.exists(pageIndex))
            return Status.PAGE_DOES_NOT_EXIST.args(pageItem.lastPageIndex() + 1);

        if (pageItem.isEmpty())
            return Status.LEADERBOARD_IS_EMPTY;

        boolean hasNext = pageItem.hasMore();
        LevelView currentUser = xpRepo.viewLevelByUserId(userId);
        List<Button> buttons = EntityContextFactory.createLevelsButtons(userId, pageIndex, hasNext);
        MessageEmbed embed = EmbedFactory.embedLevels(guild, currentUser, pageItem);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(buttons)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra o placar de líderes de nível.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "page", "A página a ser mostrada.")
                        .setRequiredRange(1, Integer.MAX_VALUE)
        );
    }
}