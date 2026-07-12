package ofc.bot.commands.impl.slash.accumulator;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.database.repository.AccumulatorPrizeRepository;
import ofc.bot.handlers.accumulator.AccumulatorImportPlanner;
import ofc.bot.handlers.accumulator.AccumulatorImportPlanner.DuplicatePolicy;
import ofc.bot.handlers.accumulator.AccumulatorImportPlanner.ImportPlan;
import ofc.bot.handlers.accumulator.AccumulatorMemberResolver;
import ofc.bot.handlers.accumulator.AccumulatorMessageFactory;
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

import java.util.List;
import java.util.Set;

@DiscordCommand(name = "accumulator import")
public class ImportAccumulatorPrizesCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportAccumulatorPrizesCommand.class);
    private final AccumulatorPrizeRepository prizeRepo;
    private final AccumulatorImportPlanner planner;
    private final AccumulatorMemberResolver memberResolver;

    public ImportAccumulatorPrizesCommand(AccumulatorPrizeRepository prizeRepo) {
        this(prizeRepo, new AccumulatorImportPlanner(), new AccumulatorMemberResolver());
    }

    ImportAccumulatorPrizesCommand(
            AccumulatorPrizeRepository prizeRepo,
            AccumulatorImportPlanner planner,
            AccumulatorMemberResolver memberResolver
    ) {
        this.prizeRepo = prizeRepo;
        this.planner = planner;
        this.memberResolver = memberResolver;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        AccumulatorPrizeConfig.ParseResult configResult = AccumulatorPrizeConfig.parse(ctx);
        if (configResult.failed()) {
            return ctx.replyEmbeds(true, configResult.failure());
        }

        String messageId = ctx.getSafeOption("message", OptionMapping::getAsString).strip();
        if (!isValidMessageId(messageId)) {
            return ctx.replyEmbeds(true, AccumulatorMessageFactory.failure(
                    "Mensagem inválida",
                    "Forneça o ID da mensagem com a lista de usuários."
            ));
        }

        DuplicatePolicy duplicatePolicy = DuplicatePolicy.fromName(ctx.getOption("duplicates", OptionMapping::getAsString));
        ctx.ack(true);
        ctx.getChannel().retrieveMessageById(messageId).queue(
                message -> importFromMessage(ctx, configResult.config(), duplicatePolicy, message),
                error -> replyMessageLookupFailure(ctx, error)
        );
        return Status.OK;
    }

    private void importFromMessage(
            SlashCommandContext ctx,
            AccumulatorPrizeConfig config,
            DuplicatePolicy duplicatePolicy,
            Message message
    ) {
        Guild guild = ctx.getGuild();
        Set<Long> pendingTargetIds = duplicatePolicy == DuplicatePolicy.FORBID
                ? prizeRepo.findPendingTargetIds(ctx.getGuildId())
                : Set.of();
        Set<Long> existingMemberIds;
        try {
            existingMemberIds = memberResolver.findExistingMemberIds(guild, planner.validTargetIds(message.getContentRaw()));
        } catch (RuntimeException e) {
            LOGGER.error("Could not resolve accumulator import members from message {} in guild {}", message.getIdLong(), ctx.getGuildId(), e);
            ctx.getSource().getHook().editOriginalEmbeds(AccumulatorMessageFactory.failure(
                    "Não foi possível validar os membros",
                    "A consulta de membros no Discord falhou. Tente novamente em alguns segundos."
            )).queue();
            return;
        }

        ImportPlan plan = planner.plan(
                message.getContentRaw(),
                duplicatePolicy,
                pendingTargetIds,
                existingMemberIds::contains
        );

        if (!plan.acceptedIds().isEmpty()) {
            long now = Bot.unixNow();
            List<AccumulatorPrize> prizes = plan.acceptedIds().stream()
                    .map(userId -> config.createPrize(ctx.getGuildId(), userId, ctx.getUserId(), now))
                    .toList();

            try {
                prizeRepo.bulkSave(prizes);
            } catch (DataAccessException e) {
                LOGGER.error("Could not import accumulator prizes from message {} in guild {}", message.getIdLong(), ctx.getGuildId(), e);
                ctx.getSource().getHook().editOriginalEmbeds(AccumulatorMessageFactory.failure(
                        "Não foi possível importar os prêmios",
                        "A caixa de prêmios pendentes não pôde ser atualizada."
                )).queue();
                return;
            }
        }

        ctx.getSource().getHook().editOriginalEmbeds(AccumulatorMessageFactory.importSuccess(
                ctx.getUser(),
                config.type(),
                plan.addedCount(),
                plan.totalIds(),
                plan.errors()
        )).queue();
    }

    private void replyMessageLookupFailure(SlashCommandContext ctx, Throwable error) {
        if (!(error instanceof ErrorResponseException)) {
            LOGGER.error("Could not retrieve accumulator import message in channel {}", ctx.getChannelId(), error);
        }

        ctx.getSource().getHook().editOriginalEmbeds(AccumulatorMessageFactory.failure(
                "Mensagem não encontrada",
                "Não encontrei essa mensagem neste canal."
        )).queue();
    }

    private boolean isValidMessageId(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return false;
        }

        try {
            return Long.parseLong(messageId) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Imports event prizes from a same-channel message with one user ID per line.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new java.util.ArrayList<>(AccumulatorPrizeConfig.options(
                new OptionData(OptionType.STRING, "message", "Message ID with one user ID per line.", true)
        ));
        options.add(new OptionData(OptionType.STRING, "duplicates", "How duplicate IDs should be handled.")
                .addChoice("Allow", DuplicatePolicy.ALLOW.name())
                .addChoice("Forbid", DuplicatePolicy.FORBID.name()));
        return options;
    }
}
