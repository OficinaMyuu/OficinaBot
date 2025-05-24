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
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

@DiscordCommand(name = "autopay enable")
public class EnableAutopaySubCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnableAutopaySubCommand.class);
    private final UserSubscriptionRepository subRepo;

    public EnableAutopaySubCommand(UserSubscriptionRepository subRepo) {
        this.subRepo = subRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        SubscriptionType type = ctx.getSafeEnumOption("type", SubscriptionType.class);
        UserSubscription sub = subRepo.findBy(userId, type);

        if (sub != null)
            return Status.AUTOPAY_SUBSCRIPTION_ALREADY_EXISTS;

        try {
            long now = Bot.unixNow();
            UserSubscription newSub = new UserSubscription(userId, type, now, now);
            subRepo.save(newSub);

            return Status.AUTOPAY_SUBSCRIPTION_CREATED_SUCCESSFULLY.args(type.getName());
        } catch (DataAccessException e) {
            LOGGER.error("Could not subscribe user {} to autopay for type {}", userId, type);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cria uma nova inscrição para débito automático.";
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