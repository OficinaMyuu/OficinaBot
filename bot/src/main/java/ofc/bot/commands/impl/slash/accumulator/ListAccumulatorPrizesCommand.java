package ofc.bot.commands.impl.slash.accumulator;

import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.sqlite.repository.AccumulatorPrizeRepository;
import ofc.bot.handlers.accumulator.AccumulatorMessageFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "accumulator list")
public class ListAccumulatorPrizesCommand extends SlashSubcommand {
    private final AccumulatorPrizeRepository prizeRepo;

    public ListAccumulatorPrizesCommand(AccumulatorPrizeRepository prizeRepo) {
        this.prizeRepo = prizeRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long guildId = ctx.getGuildId();
        PageItem<AccumulatorPrize> page = paginator(guildId).start();

        if (page.isEmpty()) {
            return Status.PAGE_IS_EMPTY;
        }

        List<net.dv8tion.jda.api.components.MessageTopLevelComponent> components =
                AccumulatorMessageFactory.createList(ctx.getGuild(), page);
        return ctx.create()
                .setUsingComponentsV2(true)
                .setComponents(components)
                .noMentions()
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Lists pending accumulated prizes.";
    }

    private Paginator<AccumulatorPrize> paginator(long guildId) {
        return Paginator.of(
                offset -> prizeRepo.findPending(guildId, offset, AccumulatorMessageFactory.PAGE_SIZE),
                () -> prizeRepo.countPending(guildId),
                AccumulatorMessageFactory.PAGE_SIZE
        );
    }
}
