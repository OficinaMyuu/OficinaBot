package ofc.bot.commands.policies;

import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.EntityPolicy;
import ofc.bot.domain.entity.enums.PolicyType;
import ofc.bot.domain.sqlite.repository.EntityPolicyRepository;
import ofc.bot.handlers.cache.PolicyService;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "policies remove", description = "Remove uma regra de um módulo do bot.")
public class RemovePolicyCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemovePolicyCommand.class);
    private final PolicyService policyService = PolicyService.getService();
    private final EntityPolicyRepository policyRepo;

    public RemovePolicyCommand(EntityPolicyRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    @Override
    public InteractionResult onSlashCommand(SlashCommandContext ctx) {
        String resource = ctx.getSafeOption("resource", OptionMapping::getAsString);
        PolicyType policy = ctx.getSafeEnumOption("policy", PolicyType.class);
        EntityPolicy rule = policyRepo.findByPolicyAndResource(policy, resource);

        if (rule == null)
            return Status.POLICY_RULE_NOT_FOUND;

        try {
            policyRepo.delete(rule);
            policyService.invalidate();

            return Status.POLICY_SUCCESSFULLY_DELETED;
        } catch (DataAccessException e) {
            LOGGER.error("Could not delete policy rule", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "policy", "A regra a ser removida.", true)
                        .addChoices(AddPolicyCommand.getPolicyTypeChoices()),

                new OptionData(OptionType.STRING, "resource", "O valor a ser removido.", true)
        );
    }
}