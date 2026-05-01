package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

@DiscordCommand(name = "events", permissions = { Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS })
public class ToggleEventsCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToggleEventsCommand.class);
    private static final String TEXT_CHANNEL_KEY = "channels.events.text.id";
    private static final String VOICE_CHANNEL_KEY = "channels.events.voice.id";

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        Role role = guild.getPublicRole();
        EventAction action = ctx.getSafeEnumOption("action", EventAction.class);
        DisconnectPolicy discPolicy = ctx.getEnumOption("disconnect", DisconnectPolicy.NONE, DisconnectPolicy.class);

        try {
            EventChannels channels = getEventChannels(guild);
            if (channels.text() == null)
                return Status.TEXT_CHANNEL_NOT_FOUND;

            if (channels.voice() == null)
                return Status.VOICE_CHANNEL_NOT_FOUND;

            if (action == EventAction.START) {
                start(channels.text(), role);
                start(channels.voice(), role);
            } else {
                end(channels.text(), role);
                end(channels.voice(), role);
                disconnectMembers(guild, channels.voice(), discPolicy);
            }
        } catch (ErrorResponseException e) {
            LOGGER.error("Could not {} event channel", action.logLabel(), e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        } catch (NoSuchElementException | NumberFormatException e) {
            LOGGER.error("Could not load event channel config", e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
        return Status.CHANNELS_STATE_TOGGLED_SUCCESSFULLY.args(action.resultLabel());
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
                new OptionData(OptionType.STRING, "action", "O que deve ser feito no canal de eventos.", true)
                        .addChoice("Start", EventAction.START.name())
                        .addChoice("End", EventAction.END.name()),
                new OptionData(OptionType.STRING, "disconnect", "Quem deve ser desconectado ao fechar o evento.")
                        .addChoice("Everyone", DisconnectPolicy.EVERYONE.name())
                        .addChoice("No one", DisconnectPolicy.NONE.name())
                        .addChoice("Keep Staff", DisconnectPolicy.KEEP_STAFF.name())
                        .addChoice("Keep Event Staff", DisconnectPolicy.KEEP_EVENT_STAFF.name())
        );
    }

    private EventChannels getEventChannels(Guild guild) {
        long textChannelId = Bot.getSafe(TEXT_CHANNEL_KEY, Long::parseLong);
        long voiceChannelId = Bot.getSafe(VOICE_CHANNEL_KEY, Long::parseLong);

        return new EventChannels(
                guild.getTextChannelById(textChannelId),
                guild.getVoiceChannelById(voiceChannelId)
        );
    }

    private void start(GuildChannel channel, Role role) {
        if (isStarted(channel, role)) return;

        channel.getPermissionContainer()
                .upsertPermissionOverride(role)
                .clear(getManagedPermission(channel))
                .complete();
    }

    private void end(GuildChannel channel, Role role) {
        if (!isStarted(channel, role)) return;

        channel.getPermissionContainer()
                .upsertPermissionOverride(role)
                .deny(getManagedPermission(channel))
                .complete();
    }

    private boolean isStarted(GuildChannel channel, Role role) {
        PermissionOverride overrides = channel.getPermissionContainer().getPermissionOverride(role);
        // Impossible today, but maybe someday the laws of nature get patched.
        if (overrides == null) return false;

        return !overrides
                .getDenied()
                .contains(getManagedPermission(channel));
    }

    private Permission getManagedPermission(GuildChannel channel) {
        return channel.getType() == ChannelType.TEXT
                ? Permission.MESSAGE_SEND
                : Permission.VOICE_CONNECT;
    }

    private void disconnectMembers(Guild guild, VoiceChannel channel, DisconnectPolicy policy) {
        if (policy == DisconnectPolicy.NONE) return;

        channel.getMembers()
                .stream()
                .filter(policy::shouldDisconnect)
                .forEach(member -> guild.kickVoiceMember(member)
                        .queue(null, new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED)));
    }

    private record EventChannels(TextChannel text, VoiceChannel voice) {}

    private enum EventAction {
        START("open", "abertos"),
        END("close", "fechados");

        private final String logLabel;
        private final String resultLabel;

        EventAction(String logLabel, String resultLabel) {
            this.logLabel = logLabel;
            this.resultLabel = resultLabel;
        }

        private String logLabel() {
            return this.logLabel;
        }

        private String resultLabel() {
            return this.resultLabel;
        }
    }

    private enum DisconnectPolicy {
        EVERYONE {
            @Override
            boolean shouldDisconnect(Member member) {
                return true;
            }
        },
        NONE {
            @Override
            boolean shouldDisconnect(Member member) {
                return false;
            }
        },
        KEEP_STAFF {
            @Override
            boolean shouldDisconnect(Member member) {
                return !Staff.isStaff(member);
            }
        },
        KEEP_EVENT_STAFF {
            @Override
            boolean shouldDisconnect(Member member) {
                return !Staff.hasRoleInScope(member, Staff.Scope.EVENTS);
            }
        };

        abstract boolean shouldDisconnect(Member member);
    }
}
