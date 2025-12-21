package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.Main;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@DiscordCommand(name = "events", permissions = Permission.MANAGE_CHANNEL)
public class ToggleEventsCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToggleEventsCommand.class);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        boolean isStart = ctx.getSafeOption("action", OptionMapping::getAsString).equals("START");
        EventType scope = ctx.getSafeEnumOption("scope", EventType.class);
        TextChannel chanTxt = scope.getTxtChan();
        VoiceChannel chanVc = scope.getVcChan();
        Guild guild = ctx.getGuild();
        Role role = guild.getPublicRole();

        if (chanTxt == null)
            return Status.TEXT_CHANNEL_NOT_FOUND;

        if (chanVc == null)
            return Status.VOICE_CHANNEL_NOT_FOUND;

        try {
            if (isStart) {
                start(chanTxt, role);
                start(chanVc, role);
            } else {
                end(chanTxt, role);
                end(chanVc, role);
            }
        } catch (ErrorResponseException e) {
            LOGGER.error("Could not {} event channel", isStart ? "open" : "close", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
        return Status.CHANNELS_STATE_TOGGLED_SUCCESSFULLY.args(isStart ? "abertos" : "fechados");
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Abra/Feche um evento.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "scope", "Qual área de eventos deve ser aberta?", true)
                        .addChoices(getScopeChoices()),
                new OptionData(OptionType.STRING, "action", "O que deve ser feito no canal de eventos.", true)
                        .addChoice("Start", "START")
                        .addChoice("End", "END")
        );
    }

    private void end(GuildChannel chan, Role role) {
        if (!isStarted(chan, role)) return;
        boolean isText = chan.getType() == ChannelType.TEXT;

        chan.getPermissionContainer()
                .upsertPermissionOverride(role)
                .deny(isText ? Permission.MESSAGE_SEND : Permission.VOICE_CONNECT)
                .complete();
    }

    private void start(GuildChannel chan, Role role) {
        if (isStarted(chan, role)) return;

        boolean isText = chan.getType() == ChannelType.TEXT;
        chan.getPermissionContainer()
                .upsertPermissionOverride(role)
                .clear(isText ? Permission.MESSAGE_SEND : Permission.VOICE_CONNECT)
                .complete();
    }

    private boolean isStarted(GuildChannel chan, Role role) {
        PermissionOverride overrides = chan.getPermissionContainer().getPermissionOverride(role);
        // Impossible, but maybe some day the laws of nature get changed
        if (overrides == null) return false;
        boolean isText = chan.getType() == ChannelType.TEXT;

        return !overrides
                .getDenied()
                .contains(isText ? Permission.MESSAGE_SEND : Permission.VOICE_CONNECT);
    }

    private List<Command.Choice> getScopeChoices() {
        return Arrays.stream(EventType.values())
                .map(et -> new Command.Choice(et.display, et.name()))
                .toList();
    }

    private enum EventType {
        RADIO("Rádio", 722532743220822126L, 640212815281520640L),
        EVENTS("Eventos", 692446195439763556L, 688179609040322626L);

        private final String display;
        private final long textId;
        private final long vcId;

        EventType(String display, long textId, long vcId) {
            this.display = display;
            this.textId = textId;
            this.vcId = vcId;
        }

        TextChannel getTxtChan() {
            return Main.getApi().getTextChannelById(this.textId);
        }

        VoiceChannel getVcChan() {
            return Main.getApi().getVoiceChannelById(this.vcId);
        }
    }
}