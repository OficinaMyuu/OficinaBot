package ofc.bot.commands.impl.slash.giveaway;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.giveaway.GiveawayEndResult;
import ofc.bot.handlers.giveaway.GiveawayComponentFactory;
import ofc.bot.handlers.giveaway.GiveawayService;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "giveaway end")
public class EndGiveawayCommand extends SlashSubcommand {
    private final GiveawayService giveawayService;

    public EndGiveawayCommand(GiveawayService giveawayService) {
        this.giveawayService = giveawayService;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String identifier = ctx.getSafeOption("id", OptionMapping::getAsString);
        GiveawayEndResult result = giveawayService.endGiveaway(identifier);

        if (result == null) {
            return ctx.reply("Sorteio não encontrado.", true);
        }

        if (!result.changed()) {
            return ctx.reply("Esse sorteio já foi encerrado.", true);
        }

        ctx.getChannel().sendMessageEmbeds(
                ofc.bot.handlers.giveaway.GiveawayMessageFactory.endedAnnouncement(
                        result.giveaway(),
                        result.winners(),
                        false
                )
        )
                .setComponents(GiveawayComponentFactory.requiresClaim(result.giveaway()) && !result.winners().isEmpty()
                        ? List.of(ActionRow.of(GiveawayComponentFactory.claimButton(result.giveaway().getGiveawayId())))
                        : List.of())
                .queue();

        return Status.DONE.setEphm(true);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Encerra um sorteio imediatamente.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "id", "ID do sorteio ou ID da mensagem.", true)
                        .setRequiredLength(1, 100)
        );
    }
}
