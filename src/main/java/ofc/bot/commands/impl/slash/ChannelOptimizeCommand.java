package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.channels.ChannelPermissionOptimizer;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DiscordCommand(name = "chanoptz", permissions = Permission.MANAGE_CHANNEL)
public class ChannelOptimizeCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelOptimizeCommand.class);
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final ChannelPermissionOptimizer optimizer = new ChannelPermissionOptimizer();

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        GuildChannel channel = ctx.getOption("channel", OptionMapping::getAsChannel);

        if (channel == null) {
            return ctx.create(true)
                    .setContent("Não consegui encontrar o canal informado.")
                    .send();
        }

        if (channel.getPermissionContainer().getIdLong() != channel.getIdLong()) {
            return ctx.create(true)
                    .setContent("Threads não são suportadas por este comando no momento.")
                    .send();
        }

        if (!ctx.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
            return ctx.create(true)
                    .setContent("Eu preciso da permissão `MANAGE_PERMISSIONS` nesse canal para revisar e aplicar a otimização.")
                    .send();
        }

        ctx.ack(false);
        var initialEmbed = EmbedFactory.embedChannelOptimizationProgress(channel, List.of(
                new EmbedFactory.TaskView("Ler overrides do canal", EmbedFactory.TaskState.RUNNING),
                new EmbedFactory.TaskView("Carregar todos os membros do servidor", EmbedFactory.TaskState.PENDING),
                new EmbedFactory.TaskView("Validar o estado atual com o JDA", EmbedFactory.TaskState.PENDING),
                new EmbedFactory.TaskView("Buscar remoções sem perda", EmbedFactory.TaskState.PENDING),
                new EmbedFactory.TaskView("Montar revisão final", EmbedFactory.TaskState.PENDING)
        ));

        ctx.create()
                .setEmbeds(initialEmbed)
                // SlashCommandsGateway already runs commands on a virtual thread,
                // and we hop to another one here so the full channel review can
                // wait on member loading and permission simulation without
                // stretching the interaction callback chain.
                .onHook(message -> EXECUTOR.execute(() -> executeAnalysis(ctx, message, channel)))
                .send();
        return Status.OK;
    }

    private void executeAnalysis(SlashCommandContext ctx, Message message, GuildChannel channel) {
        try {
            updateProgress(message, channel, 0, 1, 0, 0, 0);
            List<Member> members = channel.getGuild().loadMembers().get();

            updateProgress(message, channel, 1, 1, 0, 0, 0);
            ChannelPermissionOptimizer.AnalysisResult analysis = optimizer.analyze(channel, members);

            if (!analysis.validCurrentState()) {
                message.editMessageEmbeds(
                                EmbedFactory.embedChannelOptimizationFailure(channel, analysis.failureReason())
                        )
                        .setComponents(List.of())
                        .queue();
                return;
            }

            updateProgress(message, channel, 2, 1, 1, 0, 0);
            updateProgress(message, channel, 2, 2, 1, 1, 0);

            if (!analysis.hasChanges()) {
                message.editMessageEmbeds(
                                EmbedFactory.embedChannelOptimizationNoChanges(channel, analysis)
                        )
                        .setComponents(List.of())
                        .queue();
                return;
            }

            updateProgress(message, channel, 2, 2, 2, 1, 1);
            var approveButton = EntityContextFactory.createChannelOptimizationApproveButton(
                    ctx.getUserId(),
                    channel.getIdLong(),
                    analysis
            );

            message.editMessageEmbeds(
                            EmbedFactory.embedChannelOptimizationReview(channel, analysis)
                    )
                    .setComponents(ActionRow.of(approveButton))
                    .queue();
        } catch (Exception e) {
            LOGGER.error("Failed to analyze permissions for channel {}", channel.getIdLong(), e);
            message.editMessageEmbeds(
                            EmbedFactory.embedChannelOptimizationFailure(channel, "Não foi possível concluir a análise deste canal.")
                    )
                    .setComponents(List.of())
                    .queue();
        }
    }

    private void updateProgress(
            Message message,
            GuildChannel channel,
            int readOverrides,
            int loadMembers,
            int validateCurrentState,
            int searchRemovals,
            int buildReview
    ) {
        message.editMessageEmbeds(EmbedFactory.embedChannelOptimizationProgress(channel, List.of(
                        new EmbedFactory.TaskView("Ler overrides do canal", toState(readOverrides)),
                        new EmbedFactory.TaskView("Carregar todos os membros do servidor", toState(loadMembers)),
                        new EmbedFactory.TaskView("Validar o estado atual com o JDA", toState(validateCurrentState)),
                        new EmbedFactory.TaskView("Buscar remoções sem perda", toState(searchRemovals)),
                        new EmbedFactory.TaskView("Montar revisão final", toState(buildReview))
                )))
                .queue();
    }

    private EmbedFactory.TaskState toState(int value) {
        return switch (value) {
            case 2 -> EmbedFactory.TaskState.DONE;
            case 1 -> EmbedFactory.TaskState.RUNNING;
            default -> EmbedFactory.TaskState.PENDING;
        };
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Analisa e remove overrides redundantes de um canal sem mudar o acesso final de ninguém.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.CHANNEL, "channel", "O canal que deve ser analisado.", true)
                        .setChannelTypes(getSupportedChannelTypes())
        );
    }

    private static List<ChannelType> getSupportedChannelTypes() {
        return Arrays.stream(ChannelType.values())
                .filter(ChannelType::isGuild)
                .filter(type -> !type.name().contains("THREAD"))
                .toList();
    }
}
