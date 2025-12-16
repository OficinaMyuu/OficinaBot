package ofc.bot.commands.impl.slash.policies;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.EntityPolicy;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.entity.enums.ResourceType;
import ofc.bot.domain.sqlite.repository.EntityPolicyRepository;
import ofc.bot.handlers.cache.PolicyService;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@DiscordCommand(name = "policies add")
public class AddPolicyCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddPolicyCommand.class);
    private final PolicyService policyService = PolicyService.getService();
    private final EntityPolicyRepository policyRepo;

    public AddPolicyCommand(EntityPolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        PolicyType policy = ctx.getSafeEnumOption("policy", PolicyType.class);
        ResourceType resType = ctx.getSafeEnumOption("resource-type", ResourceType.class);
        String resource = ctx.getOption("resource", OptionMapping::getAsString);

        if (!policy.isSupported(resType))
            return Status.UNSUPPORTED_RESOURCE_TYPE.args(policy, resource, formatResourceTypes(policy.getSupportedTypes()));

        try {
            EntityPolicy entity = new EntityPolicy(policy, resType, resource);
            policyRepo.save(entity);
            policyService.invalidate();

            return Status.POLICY_SUCCESSFULLY_CREATED;
        } catch (DataAccessException e) {
            LOGGER.error("Could not save policy rule", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Adiciona uma regra à um módulo do bot.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "policy", "A regra a ser definida.", true)
                        .addChoices(getPolicyTypeChoices()),

                new OptionData(OptionType.STRING, "resource-type", "O tipo de valor a ser fornecido.", true)
                        .addChoices(getResourceTypeChoices()),

                new OptionData(OptionType.STRING, "resource", "O valor a ser definido.", true, true)
        );
    }

    private String formatResourceTypes(List<ResourceType> types) {
        return types.stream()
                .map(rt -> String.format("`%s`", Bot.upperFirst(rt.name().toLowerCase())))
                .collect(Collectors.joining(", "));
    }

    static List<Command.Choice> getPolicyTypeChoices() {
        return Arrays.stream(PolicyType.values())
                .map(pt -> new Command.Choice(pt.getDescription(), pt.name()))
                .toList();
    }

    static List<Command.Choice> getResourceTypeChoices() {
        return Arrays.stream(ResourceType.values())
                .map(rt -> new Command.Choice(Bot.upperFirst(rt.name().toLowerCase()), rt.name()))
                .toList();
    }
}