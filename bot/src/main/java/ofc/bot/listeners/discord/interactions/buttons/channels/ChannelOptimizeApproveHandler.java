package ofc.bot.listeners.discord.interactions.buttons.channels;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import ofc.bot.handlers.channels.ChannelPermissionOptimizer;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InteractionHandler(scope = Scopes.Channels.APPROVE_OPTIMIZATION, autoResponseType = AutoResponseType.DEFER_EDIT)
public class ChannelOptimizeApproveHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelOptimizeApproveHandler.class);

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        long channelId = ctx.get("channel_id");
        ChannelPermissionOptimizer.AnalysisResult plan = ctx.get("plan");
        GuildChannel channel = ctx.getGuild().getGuildChannelById(channelId);

        if (channel == null) {
            ctx.editMessageEmbeds(
                    EmbedFactory.embedChannelOptimizationFailure(null, "O canal desta revisão não existe mais.")
            ).setComponents(java.util.List.of()).queue();
            return ofc.bot.handlers.interactions.commands.responses.states.Status.OK;
        }

        if (!ctx.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
            ctx.editMessageEmbeds(
                    EmbedFactory.embedChannelOptimizationFailure(channel, "Eu perdi a permissão `MANAGE_PERMISSIONS` nesse canal.")
            ).setComponents(java.util.List.of()).queue();
            return ofc.bot.handlers.interactions.commands.responses.states.Status.OK;
        }

        String currentSignature = ChannelPermissionOptimizer.computeSignature(
                channel.getPermissionContainer().getPermissionOverrides().stream()
                        .sorted(java.util.Comparator
                                .comparing((PermissionOverride override) -> override.isMemberOverride() ? 1 : 0)
                                .thenComparingLong(PermissionOverride::getIdLong))
                        .map(override -> new ChannelPermissionOptimizer.OverrideState(
                                override.getIdLong(),
                                override.isMemberOverride()
                                        ? ChannelPermissionOptimizer.HolderType.MEMBER
                                        : ChannelPermissionOptimizer.HolderType.ROLE,
                                override.getAllowedRaw(),
                                override.getDeniedRaw(),
                                override.isMemberOverride() ? "Membro " + override.getId() : "Cargo " + override.getId()
                        ))
                        .toList()
        );

        if (!plan.originalSignature().equals(currentSignature)) {
            ctx.editMessageEmbeds(
                    EmbedFactory.embedChannelOptimizationFailure(
                            channel,
                            "Os overrides mudaram depois da revisão. Rode `/chanoptz` novamente para gerar uma nova análise."
                    )
            ).setComponents(java.util.List.of()).queue();
            return ofc.bot.handlers.interactions.commands.responses.states.Status.OK;
        }

        try {
            for (ChannelPermissionOptimizer.OverrideChange change : plan.changes()) {
                PermissionOverride current = channel.getPermissionContainer().getPermissionOverrides().stream()
                        .filter(override -> override.getIdLong() == change.before().holderId())
                        .findFirst()
                        .orElse(null);

                if (current == null) {
                    throw new IllegalStateException("Override vanished during approval");
                }

                if (change.deletesOverride()) {
                    current.delete().complete();
                    continue;
                }

                var manager = current.getManager();
                for (Permission permission : ChannelPermissionOptimizer.permissionsOf(change.removedAllowedRaw())) {
                    manager = manager.clear(permission);
                }

                for (Permission permission : ChannelPermissionOptimizer.permissionsOf(change.removedDeniedRaw())) {
                    manager = manager.clear(permission);
                }

                manager.complete();
            }

            ctx.editMessageEmbeds(
                    EmbedFactory.embedChannelOptimizationApplied(channel, plan)
            ).setComponents(java.util.List.of()).queue();
        } catch (Exception e) {
            LOGGER.error("Failed to apply channel optimization for {}", channelId, e);
            ctx.editMessageEmbeds(
                    EmbedFactory.embedChannelOptimizationFailure(channel, "Não foi possível aplicar as mudanças revisadas.")
            ).setComponents(java.util.List.of()).queue();
        }

        return ofc.bot.handlers.interactions.commands.responses.states.Status.OK;
    }
}
