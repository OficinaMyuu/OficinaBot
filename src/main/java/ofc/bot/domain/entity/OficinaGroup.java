package ofc.bot.domain.entity;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import ofc.bot.Main;
import ofc.bot.domain.entity.enums.RentStatus;
import ofc.bot.domain.tables.OficinaGroupsTable;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.util.content.Staff;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.LocalDate;
import java.util.List;

import static net.dv8tion.jda.api.Permission.*;

public class OficinaGroup extends OficinaRecord<OficinaGroup> {
    private static final OficinaGroupsTable GROUPS = OficinaGroupsTable.OFICINA_GROUPS;

    public static final List<Permission> PERMS_ALLOW_TEXT_CHANNEL;
    public static final List<Permission> PERMS_ALLOW_VOICE_CHANNEL;

    public static final String ROLE_NAME_FORMAT = "⠀⠀⠀⠀⠀⠀%s⠀⠀⠀⠀⠀⠀";
    public static final String TEXT_CHANNEL_NAME_FORMAT = "%s｜%s";
    public static final String VOICE_CHANNEL_NAME_FORMAT = "%s・%s";
    public static final float REFUND_PERCENT = .15F;
    public static final float DAILY_INVOICE_PENALTY_RATE = .15F;
    public static final int INVOICE_DUE_DAY = 10;
    public static final long ANCHOR_GROUP_ROLE_ID = 596784802150088704L;
    public static final long TEXT_CATEGORY_ID = 648431232429850664L;
    public static final long VOICE_CATEGORY_ID = 623004940918325248L;
    public static final int RENT_AMOUNT_PER_MEMBER = 1000;
    public static final int DEFAULT_VOICE_USERS_LIMIT = 50;
    public static final int INITIAL_SLOTS = 4;
    public static final int MAX_NAME_LENGTH = 30;
    public static final int MIN_NAME_LENGTH = 3;

    public OficinaGroup() {
        super(GROUPS);
    }

    public OficinaGroup(String name, long ownerId, long guildId, RentStatus rentStatus, boolean hasFreeAccess) {
        this();
        set(GROUPS.NAME, name);
        set(GROUPS.OWNER_ID, ownerId);
        set(GROUPS.GUILD_ID, guildId);
        set(GROUPS.HAS_FREE_ACCESS, hasFreeAccess);
        set(GROUPS.RENT_STATUS, rentStatus.toString());
    }

    /**
     * Checks if the given member, when creating a group, will
     * be charged monthly.
     * <p>
     * <b>Note:</b> You should only use the value returned by this method to
     * instantiate a new {@link OficinaGroup}.
     * If you want to check if a group is rent recurring, use
     * {@link #getRentStatus()}  OficinaGroup.getRentStatus()} and check
     * if its equal to {@link RentStatus#FREE} or {@link RentStatus#TRIAL}.
     *
     * @param member the member that will be (theoretically) creating the group.
     * @return {@code true} if the group members will be charged monthly,
     *         {@code false} otherwise.
     */
    public static boolean isRentRecurring(Member member) {
        if (member == null)
            throw new IllegalArgumentException("Member cannot be null");

        if (member.hasPermission(MANAGE_SERVER)) return false;

        Role hashiras = Staff.ALMIRANTES_FROTA.role();
        List<Role> allowed = findAllAbove(hashiras);

        return allowed.stream().noneMatch(r -> member.getRoles().contains(r));
    }

    /**
     * Checks whether the provided member should have access to all group-related resources
     * for free (does not check for rent eligibility).
     * <p>
     * Currently, this method just checks if the member has the {@link Permission#MANAGE_SERVER}
     * permission, but this behavior may change in the future.
     * <p>
     * <b>Note:</b> The returned value of this method should only be used to instantiate a new
     * {@link OficinaGroup} object. If you want to check if a group has access to free resources,
     * you should call the non-static {@link #hasFreeAccess() OficinaGroup.hasFreeAccess()}
     *
     * @param member the member to be checked for free eligibility.
     * @return {@code true} if the member has free access for all group-related resources,
     *         {@code false} otherwise.
     */
    public static boolean hasFreeAccess(Member member) {
        if (member == null)
            throw new IllegalArgumentException("Member cannot be null");

        return member.hasPermission(MANAGE_SERVER);
    }

    public int getId() {
        return get(GROUPS.ID);
    }

    public long getOwnerId() {
        return get(GROUPS.OWNER_ID);
    }

    public String getOwnerAsMention() {
        return "<@" + getOwnerId() + '>';
    }

    public long getGuildId() {
        return get(GROUPS.GUILD_ID);
    }

    public long getRoleId() {
        return get(GROUPS.ROLE_ID);
    }

    public Role resolveRole() {
        return Main.getApi().getRoleById(getRoleId());
    }

    /**
     * This method attempts to resolve the color of the group.
     * <p>
     * Such information is not stored in the database, instead,
     * we get it from the group {@link Role}.
     * <p>
     * The returned value may be {@code 0} for 2 reasons:
     * <ul>
     *   <li>The group {@link Role} was not found.</li>
     *   <li>There is no color set.</li>
     * </ul>
     *
     * @return the color of the group, or {@code 0}.
     */
    public int resolveColor() {
        long roleId = getRoleId();
        JDA api = Main.getApi();
        Role role = api.getRoleById(roleId);

        if (role == null) return 0;

        Color color = role.getColor();
        return color == null ? 0 : color.getRGB();
    }

    public long getTextChannelId() {
        return get(GROUPS.TEXT_CHANNEL_ID);
    }

    public boolean hasTextChannel() {
        return get(GROUPS.TEXT_CHANNEL_ID) != null;
    }

    public TextChannel getTextChannel() {
        Long chanId = get(GROUPS.TEXT_CHANNEL_ID);
        return chanId == null ? null : Main.getApi().getTextChannelById(chanId);
    }

    /**
     * This method returns the name of the channel as it should be,
     * per the format defined at {@link #TEXT_CHANNEL_NAME_FORMAT}.
     * <p>
     * The value returned may differ from the real channel on Discord,
     * as it may have its name updated by a moderator.
     *
     * @return the name of the text channel formatted per the documentation.
     */
    public String getTextChannelName() {
        return String.format(TEXT_CHANNEL_NAME_FORMAT, getEmoji(), getName());
    }

    public void sendMessagef(String content, Object... args) {
        TextChannel textchan = getTextChannel();

        if (textchan != null)
            textchan.sendMessageFormat(content, args).queue();
    }

    public long getVoiceChannelId() {
        return get(GROUPS.VOICE_CHANNEL_ID);
    }

    public boolean hasVoiceChannel() {
        return get(GROUPS.VOICE_CHANNEL_ID) != null;
    }

    public VoiceChannel getVoiceChannel() {
        Long voiceId = get(GROUPS.VOICE_CHANNEL_ID);
        return voiceId == null ? null : Main.getApi().getVoiceChannelById(voiceId);
    }

    /**
     * This method returns the name of the channel as it should be,
     * per the format defined at {@link #VOICE_CHANNEL_NAME_FORMAT}.
     * <p>
     * The value returned may differ from the real channel on Discord,
     * as it may have its name updated by a moderator.
     *
     * @return the name of the voice channel formatted per the documentation.
     */
    public String getVoiceChannelName() {
        return String.format(VOICE_CHANNEL_NAME_FORMAT, getEmoji(), getName());
    }

    public String getChannelName(ChannelType channelType) {
        return switch (channelType) {
            case TEXT -> getTextChannelName();
            case VOICE -> getVoiceChannelName();
            default -> null;
        };
    }

    /**
     * Whether the group has either a {@link net.dv8tion.jda.api.entities.channel.concrete.TextChannel TextChannel}
     * or a {@link net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel VoiceChannel}.
     *
     * @return {@code true} if the group has at least one of the two types of channel,
     *         {@code false} otherwise.
     */
    public boolean hasChannels() {
        return hasTextChannel() || hasVoiceChannel();
    }

    public String getName() {
        return get(GROUPS.NAME);
    }

    public String getEmoji() {
        return get(GROUPS.EMOJI);
    }

    public CurrencyType getCurrency() {
        String curr = get(GROUPS.CURRENCY);
        return CurrencyType.fromName(curr);
    }

    public int getAmountPaid() {
        return get(GROUPS.AMOUNT_PAID);
    }

    public long getInvoiceAmount() {
        return get(GROUPS.INVOICE_AMOUNT);
    }

    public double getRefundPercent() {
        return get(GROUPS.REFUND_PERCENT);
    }

    /**
     * Checks whether the current group is eligible for free resources.
     * <p>
     * These groups don't have to pay for any items they want;
     * if you want to know whether this group is free of monthly rent,
     * call {@link #getRentStatus()} and check if its equal to {@link RentStatus#FREE}.
     *
     * @return {@code true} if the group don't have to pay for any items,
     *         {@code false} otherwise.
     */
    public boolean hasFreeAccess() {
        Boolean free = get(GROUPS.HAS_FREE_ACCESS);
        return free != null && free;
    }

    public RentStatus getRentStatus() {
        String status = get(GROUPS.RENT_STATUS);
        return RentStatus.valueOf(status);
    }

    /**
     * Checks if the group will be monthly charged to pay their rent.
     * <p>
     * <b>Note:</b> This method <b><u>DOES NOT</u></b> check if the group
     * is in the trial period.
     *
     * @return {@code true} if the group is incurring monthly rent charges,
     *         {@code false} otherwise.
     */
    public boolean isRentRecurring() {
        return getRentStatus() != RentStatus.FREE;
    }

    public boolean isTrial() {
        return getRentStatus() == RentStatus.TRIAL;
    }

    public long getTimeCreated() {
        return get(GROUPS.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(GROUPS.UPDATED_AT);
    }

    public OficinaGroup setOwnerId(long ownerId) {
        set(GROUPS.OWNER_ID, ownerId);
        return this;
    }

    public OficinaGroup setGuildId(long guildId) {
        set(GROUPS.GUILD_ID, guildId);
        return this;
    }

    public OficinaGroup setRoleId(long roleId) {
        set(GROUPS.ROLE_ID, roleId);
        return this;
    }

    public OficinaGroup setTextChannelId(long chanId) {
        set(GROUPS.TEXT_CHANNEL_ID, chanId);
        return this;
    }

    public OficinaGroup setVoiceChannelId(long chanId) {
        set(GROUPS.VOICE_CHANNEL_ID, chanId);
        return this;
    }

    public OficinaGroup setChannelId(ChannelType channelType, long id) {
        return switch (channelType) {
            case TEXT -> setTextChannelId(id);
            case VOICE -> setVoiceChannelId(id);
            default -> throw new IllegalArgumentException("Invalid channel type: " + channelType);
        };
    }

    public OficinaGroup setName(String name) {
        set(GROUPS.NAME, name);
        return this;
    }

    public OficinaGroup setEmoji(String emoji) {
        set(GROUPS.EMOJI, emoji);
        return this;
    }

    public OficinaGroup setCurrency(CurrencyType curr) {
        set(GROUPS.CURRENCY, curr.toString());
        return this;
    }

    public OficinaGroup setAmountPaid(int value) {
        set(GROUPS.AMOUNT_PAID, value);
        return this;
    }

    public OficinaGroup setInvoiceAmount(long value) {
        set(GROUPS.INVOICE_AMOUNT, value);
        return this;
    }

    public OficinaGroup setRefundPercent(double value) {
        set(GROUPS.REFUND_PERCENT, value);
        return this;
    }

    public OficinaGroup setFreeAccess(boolean flag) {
        set(GROUPS.HAS_FREE_ACCESS, flag);
        return this;
    }

    public OficinaGroup setRentStatus(RentStatus status) {
        set(GROUPS.RENT_STATUS, status.toString());
        return this;
    }

    public OficinaGroup setTimeCreated(long timestamp) {
        set(GROUPS.CREATED_AT, timestamp);
        return this;
    }

    @NotNull
    public OficinaGroup setLastUpdated(long timestamp) {
        set(GROUPS.UPDATED_AT, timestamp);
        return this;
    }

    public int getDaysLate() {
        int today = LocalDate.now().getDayOfMonth();
        return Math.max(today - INVOICE_DUE_DAY, 0);
    }

    /**
     * Checks if the rent payment is considered late. A rent payment is deemed late if
     * it has not been paid by the {@value #INVOICE_DUE_DAY}th day of the current month.
     * <p>
     * This method determines whether the rent payment is late based on the following conditions:
     * <ul>
     *   <li>If {@link #getRentStatus()} returns {@link RentStatus#NOT_PAID}, this method will return {@code false},
     *       because the rent has not been paid at all and thus cannot be considered "late".</li>
     *   <li>If the rent status is {@link RentStatus#PENDING} and the current day of the month
     *       exceeds {@value #INVOICE_DUE_DAY}, then {@code true} is returned.</li>
     * </ul>
     *
     * @return {@code true} if the rent payment is late, {@code false} otherwise.
     * @see #getRentStatus()
     */
    public boolean isRentLate() {
        return getRentStatus() == RentStatus.PENDING && getDaysLate() > 0;
    }

    public synchronized long calcRawRent(List<Member> members) {
        if (!isRentRecurring()) return 0;

        Role role = Main.getApi().getRoleById(getRoleId());

        if (role == null)
            throw new IllegalStateException("Role of group " + getName() + " does not exist");

        Role anchor = Staff.ALMIRANTES_FROTA.role();
        List<Role> privilegedRoles = findAllAbove(anchor);

        return members.stream()
                .filter(m -> m.getRoles().stream().noneMatch(privilegedRoles::contains))
                .count() * RENT_AMOUNT_PER_MEMBER;
    }

    public long calcCurrentInvoice() {
        int daysLate = getDaysLate();
        long raw = getInvoiceAmount();

        for (int i = 0; i < daysLate; i++) {
            raw += Math.round(raw * DAILY_INVOICE_PENALTY_RATE);
        }
        return raw;
    }

    private static List<Role> findAllAbove(Role role) {
        Guild guild = role.getGuild();

        return guild.getRoles()
                .stream()
                .filter((r) -> r.getPosition() >= role.getPosition())
                .toList();
    }

    static {
        PERMS_ALLOW_TEXT_CHANNEL = List.of(
                VIEW_CHANNEL, MESSAGE_EMBED_LINKS, MESSAGE_ATTACH_FILES,
                MESSAGE_ATTACH_VOICE_MESSAGE, MESSAGE_EXT_EMOJI, MESSAGE_EXT_STICKER
        );

        PERMS_ALLOW_VOICE_CHANNEL = List.of(
                VOICE_CONNECT, VOICE_STREAM, USE_EMBEDDED_ACTIVITIES
        );
    }
}
