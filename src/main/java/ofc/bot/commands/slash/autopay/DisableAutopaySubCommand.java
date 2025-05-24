package ofc.bot.commands.slash.autopay;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.UserSubscription;
import ofc.bot.domain.entity.enums.SubscriptionType;
import ofc.bot.domain.sqlite.repository.UserSubscriptionRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

@DiscordCommand(name = "autopay disable")
public class DisableAutopaySubCommand extends SlashCommand {
    private final UserSubscriptionRepository subRepo;

    public DisableAutopaySubCommand(UserSubscriptionRepository subRepo) {
        this.subRepo = subRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        SubscriptionType type = ctx.getSafeEnumOption("type", SubscriptionType.class);
        UserSubscription sub = subRepo.findBy(userId, type);

        if (sub == null)
            return Status.AUTOPAY_SUBSCRIPTION_DOES_NOT_EXIST;







        return null;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Remove uma inscrição do débito automático.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "type", "O tipo de inscrição.", true)
                        .addChoices(getTypeChoices())
        );
    }

    private List<Command.Choice> getTypeChoices() {
        return Stream.of(SubscriptionType.values())
                .map(s -> new Command.Choice(s.getName(), s.name()))
                .toList();
    }
}