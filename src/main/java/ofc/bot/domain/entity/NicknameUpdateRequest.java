package ofc.bot.domain.entity;

import ofc.bot.domain.entity.enums.NicknameRequestStatus;
import ofc.bot.domain.tables.NicknameUpdateRequestsTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NicknameUpdateRequest extends OficinaRecord<NicknameUpdateRequest> {
    private static final NicknameUpdateRequestsTable NICK_REQUESTS =
            NicknameUpdateRequestsTable.NICKNAME_UPDATE_REQUESTS;

    public NicknameUpdateRequest() {
        super(NICK_REQUESTS);
    }

    public NicknameUpdateRequest(
            @NotNull String requestId,
            long guildId,
            long channelId,
            long messageId,
            long targetUserId,
            long submittedById,
            @NotNull String nickname,
            @NotNull String approveButtonId,
            @NotNull String rejectButtonId,
            @NotNull NicknameRequestStatus status,
            @Nullable String emojiApprovalSummary,
            @Nullable String unauthorizedSummary,
            long createdAt,
            long updatedAt
    ) {
        this();
        setRequestId(requestId);
        setGuildId(guildId);
        setChannelId(channelId);
        setMessageId(messageId);
        setTargetUserId(targetUserId);
        setSubmittedById(submittedById);
        setNickname(nickname);
        setApproveButtonId(approveButtonId);
        setRejectButtonId(rejectButtonId);
        setStatus(status);
        setEmojiApprovalSummary(emojiApprovalSummary);
        setUnauthorizedSummary(unauthorizedSummary);
        setTimeCreated(createdAt);
        setLastUpdated(updatedAt);
    }

    public String getRequestId() {
        return get(NICK_REQUESTS.REQUEST_ID);
    }

    public long getGuildId() {
        return get(NICK_REQUESTS.GUILD_ID);
    }

    public long getChannelId() {
        return get(NICK_REQUESTS.CHANNEL_ID);
    }

    public long getMessageId() {
        return get(NICK_REQUESTS.MESSAGE_ID);
    }

    public long getTargetUserId() {
        return get(NICK_REQUESTS.TARGET_USER_ID);
    }

    public long getSubmittedById() {
        return get(NICK_REQUESTS.SUBMITTED_BY_ID);
    }

    public String getNickname() {
        return get(NICK_REQUESTS.NICKNAME);
    }

    public String getApproveButtonId() {
        return get(NICK_REQUESTS.APPROVE_BUTTON_ID);
    }

    public String getRejectButtonId() {
        return get(NICK_REQUESTS.REJECT_BUTTON_ID);
    }

    public NicknameRequestStatus getStatus() {
        return NicknameRequestStatus.valueOf(get(NICK_REQUESTS.STATUS));
    }

    public long getDecisionAuthorId() {
        Long value = get(NICK_REQUESTS.DECISION_AUTHOR_ID);
        return value == null ? 0 : value;
    }

    public long getDecidedAt() {
        Long value = get(NICK_REQUESTS.DECIDED_AT);
        return value == null ? 0 : value;
    }

    public String getEmojiApprovalSummary() {
        return get(NICK_REQUESTS.EMOJI_APPROVAL_SUMMARY);
    }

    public String getUnauthorizedSummary() {
        return get(NICK_REQUESTS.UNAUTHORIZED_SUMMARY);
    }

    public long getTimeCreated() {
        return get(NICK_REQUESTS.CREATED_AT);
    }

    @Override
    public long getLastUpdated() {
        return get(NICK_REQUESTS.UPDATED_AT);
    }

    public NicknameUpdateRequest setRequestId(@NotNull String requestId) {
        set(NICK_REQUESTS.REQUEST_ID, requestId);
        return this;
    }

    public NicknameUpdateRequest setGuildId(long guildId) {
        set(NICK_REQUESTS.GUILD_ID, guildId);
        return this;
    }

    public NicknameUpdateRequest setChannelId(long channelId) {
        set(NICK_REQUESTS.CHANNEL_ID, channelId);
        return this;
    }

    public NicknameUpdateRequest setMessageId(long messageId) {
        set(NICK_REQUESTS.MESSAGE_ID, messageId);
        return this;
    }

    public NicknameUpdateRequest setTargetUserId(long targetUserId) {
        set(NICK_REQUESTS.TARGET_USER_ID, targetUserId);
        return this;
    }

    public NicknameUpdateRequest setSubmittedById(long submittedById) {
        set(NICK_REQUESTS.SUBMITTED_BY_ID, submittedById);
        return this;
    }

    public NicknameUpdateRequest setNickname(@NotNull String nickname) {
        set(NICK_REQUESTS.NICKNAME, nickname);
        return this;
    }

    public NicknameUpdateRequest setApproveButtonId(@NotNull String approveButtonId) {
        set(NICK_REQUESTS.APPROVE_BUTTON_ID, approveButtonId);
        return this;
    }

    public NicknameUpdateRequest setRejectButtonId(@NotNull String rejectButtonId) {
        set(NICK_REQUESTS.REJECT_BUTTON_ID, rejectButtonId);
        return this;
    }

    public NicknameUpdateRequest setStatus(@NotNull NicknameRequestStatus status) {
        set(NICK_REQUESTS.STATUS, status.name());
        return this;
    }

    public NicknameUpdateRequest setDecisionAuthorId(@Nullable Long userId) {
        set(NICK_REQUESTS.DECISION_AUTHOR_ID, userId);
        return this;
    }

    public NicknameUpdateRequest setDecidedAt(@Nullable Long decidedAt) {
        set(NICK_REQUESTS.DECIDED_AT, decidedAt);
        return this;
    }

    public NicknameUpdateRequest setEmojiApprovalSummary(@Nullable String summary) {
        set(NICK_REQUESTS.EMOJI_APPROVAL_SUMMARY, summary);
        return this;
    }

    public NicknameUpdateRequest setUnauthorizedSummary(@Nullable String summary) {
        set(NICK_REQUESTS.UNAUTHORIZED_SUMMARY, summary);
        return this;
    }

    public NicknameUpdateRequest setTimeCreated(long createdAt) {
        set(NICK_REQUESTS.CREATED_AT, createdAt);
        return this;
    }

    @NotNull
    @Override
    public NicknameUpdateRequest setLastUpdated(long updatedAt) {
        set(NICK_REQUESTS.UPDATED_AT, updatedAt);
        return this;
    }
}
