package ofc.bot.handlers.nick;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.domain.entity.NicknameUpdateRequest;
import ofc.bot.domain.entity.enums.NicknameRequestStatus;
import ofc.bot.domain.database.repository.NicknameUpdateRequestRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class NicknameRequestDispatcher {
    public static final String STAFF_CHANNEL_KEY = "channels.staff-nick-update.id";
    private static final Logger LOGGER = LoggerFactory.getLogger(NicknameRequestDispatcher.class);

    private final NicknameUpdateRequestRepository requestRepo;

    public NicknameRequestDispatcher(NicknameUpdateRequestRepository requestRepo) {
        this.requestRepo = Objects.requireNonNull(requestRepo);
    }

    public void submit(
            @NotNull Guild guild,
            @NotNull Member target,
            @NotNull User submittedBy,
            @NotNull String nickname,
            @NotNull NicknameEmojiPolicy.NicknameEmojiReport report,
            boolean forced,
            @NotNull Consumer<NicknameUpdateRequest> success,
            @NotNull Consumer<Throwable> failure
    ) {
        TextChannel staffChannel = resolveStaffChannel(guild);
        if (staffChannel == null) {
            failure.accept(new IllegalStateException("Approval channel not found."));
            return;
        }

        String requestId = UUID.randomUUID().toString();
        String approveButtonId = "nick-" + requestId + "-approve";
        String rejectButtonId = "nick-" + requestId + "-reject";
        Button approve = Button.of(ButtonStyle.SUCCESS, approveButtonId, "Aprovar");
        Button reject = Button.of(ButtonStyle.DANGER, rejectButtonId, "Rejeitar");
        MessageEmbed embed = EmbedFactory.embedNicknameApprovalPending(target, submittedBy, nickname, report, forced);

        staffChannel.sendMessageEmbeds(embed)
                .setComponents(ActionRow.of(approve, reject))
                .queue(message -> {
                    NicknameUpdateRequest request = createRequest(
                            requestId,
                            guild,
                            message,
                            target,
                            submittedBy,
                            nickname,
                            approveButtonId,
                            rejectButtonId,
                            report,
                            forced
                    );

                    requestRepo.save(request);
                    success.accept(request);
                }, error -> {
                    LOGGER.error("Failed to send nickname request to approval channel", error);
                    failure.accept(error);
                });
    }

    private TextChannel resolveStaffChannel(Guild guild) {
        Long channelId = Bot.get(STAFF_CHANNEL_KEY, Long::parseLong);
        return channelId == null ? null : guild.getTextChannelById(channelId);
    }

    private NicknameUpdateRequest createRequest(
            String requestId,
            Guild guild,
            Message message,
            Member target,
            User submittedBy,
            String nickname,
            String approveButtonId,
            String rejectButtonId,
            NicknameEmojiPolicy.NicknameEmojiReport report,
            boolean forced
    ) {
        long now = Bot.nowMillis();

        return new NicknameUpdateRequest(
                requestId,
                guild.getIdLong(),
                message.getChannelIdLong(),
                message.getIdLong(),
                target.getIdLong(),
                submittedBy.getIdLong(),
                nickname,
                approveButtonId,
                rejectButtonId,
                NicknameRequestStatus.PENDING,
                report.approvedSummary(),
                forced ? report.unauthorizedSummary() : null,
                now,
                now
        );
    }

    public static void clearRequestComponents(Message message) {
        message.editMessageComponents(List.of())
                .useComponentsV2(message.isUsingComponentsV2())
                .queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
    }
}
