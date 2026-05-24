package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@DiscordCommand(name = "wakeup", permissions = Permission.ADMINISTRATOR)
public class WakeupCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WakeupCommand.class);
    private static final AtomicLong currentUserId = new AtomicLong();
    private static final Random RANDOM = new Random();
    private static final int DEFAULT_TRIPS = 5;
    private static final int SLEEP_MILLIS = 300;

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        VoiceChannel targetChannel = ctx.getOption("channel", opt -> {
            GuildChannelUnion tmp = opt.getAsChannel();
            return tmp instanceof VoiceChannel ? tmp.asVoiceChannel() : null;
        });

        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        int trips = ctx.getOption("trips", DEFAULT_TRIPS, OptionMapping::getAsInt);

        if (target == null) {
            return Status.MEMBER_NOT_FOUND;
        }

        GuildVoiceState voiceState = target.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            return Status.USER_NOT_CONNECTED_TO_VOICE_CHANNELS;
        }

        long targetId = target.getIdLong();
        AudioChannel origChan = voiceState.getChannel();

        boolean monitorMute = voiceState.isSelfMuted();

        if (!currentUserId.compareAndSet(0, targetId)) {
            return Status.ALREADY_WAKINGUP_OTHER_USERS;
        }

        CompletableFuture.runAsync(() -> executeWakeupTask(target, origChan, targetChannel, trips, monitorMute));

        return Status.WAKING_UP_USER_SUCCESSFULLY;
    }

    /**
     * Handles the entire waking up loop, keeping onCommand clean.
     */
    private void executeWakeupTask(Member target, AudioChannel origChan, AudioChannel staticDest, int trips, boolean monitorMute) {
        Guild guild = target.getGuild();
        long targetId = target.getIdLong();

        try {
            for (int i = 0; i < trips; i++) {
                if (shouldStop(target, monitorMute)) break;

                AudioChannel dest = staticDest != null ? staticDest : getRandomVoiceChannel(guild, origChan);
                if (dest == null) break;

                guild.moveVoiceMember(target, dest).complete();
                sleep();

                guild.moveVoiceMember(target, origChan).complete();
                sleep();
            }
        } catch (Exception e) {
            LOGGER.error("Error while waking up user {}", targetId, e);
        } finally {
            currentUserId.set(0);
            ensureUserInOriginalChannel(target, origChan);
        }
    }

    /**
     * Strictly evaluates if the process should stop. No side effects (no moving users here).
     */
    private boolean shouldStop(Member target, boolean monitorMute) {
        GuildVoiceState state = target.getVoiceState();

        // Stop if they disconnected entirely
        if (state == null || !state.inAudioChannel())
            return true;

        // Stop if we are monitoring their mute state, and they unmuted
        return monitorMute && !state.isSelfMuted();
    }

    /**
     * Safely attempts to move the user back to the starting channel as a final cleanup step.
     */
    private void ensureUserInOriginalChannel(Member target, AudioChannel origChan) {
        GuildVoiceState finalState = target.getVoiceState();

        if (finalState == null || finalState.inAudioChannel()) return;

        AudioChannel currentChan = finalState.getChannel();
        if (currentChan != null && !currentChan.equals(origChan)) {
            try {
                target.getGuild().moveVoiceMember(target, origChan).complete();
            } catch (Exception e) {
                LOGGER.warn("Could not return user {} to original channel during cleanup", target.getIdLong(), e);
            }
        }
    }

    private AudioChannel getRandomVoiceChannel(Guild guild, AudioChannel excludeChannel) {
        List<VoiceChannel> validChannels = guild.getVoiceChannels()
                .stream()
                .filter(c -> !c.equals(excludeChannel))
                .toList();

        if (validChannels.isEmpty()) return null;

        return validChannels.get(RANDOM.nextInt(validChannels.size()));
    }

    private void sleep() {
        try {
            Thread.sleep(SLEEP_MILLIS);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to sleep for {}ms, interrupting...", SLEEP_MILLIS, e);
            Thread.currentThread().interrupt();
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Move o usuário de call a quantidade de vezes definidas.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a ser movido de call.", true),
                new OptionData(OptionType.INTEGER, "trips", "Quantas vezes o membro será movido (1 = ida e volta).")
                        .setRequiredRange(1, 50),

                new OptionData(OptionType.CHANNEL, "channel", "Para qual canal o usuário será movido.")
                        .setChannelTypes(ChannelType.VOICE)
        );
    }
}