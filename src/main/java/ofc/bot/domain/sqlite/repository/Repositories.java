package ofc.bot.domain.sqlite.repository;

import ofc.bot.domain.sqlite.DB;
import org.jooq.DSLContext;

public final class Repositories {
    private static AppUserBanRepository appUserBanRepository;
    private static AutomodActionRepository automodActionRepository;
    private static BankTransactionRepository bankTransactionRepository;
    private static BetGameRepository gameRepository;
    private static BirthdayRepository birthdayRepository;
    private static BlockedWordRepository blockedWordRepository;
    private static ColorRoleStateRepository colorRoleStateRepository;
    private static CommandHistoryRepository commandHistoryRepository;
    private static CustomUserinfoRepository customUserinfoRepository;
    private static DiscordMessageRepository discordMessageRepository;
    private static DiscordMessageUpdateRepository discordMessageUpdateRepository;
    private static EntityPolicyRepository entityPolicyRepository;
    private static FormerMemberRoleRepository formerMemberRoleRepository;
    private static GameParticipantRepository gameParticipantRepository;
    private static GroupBotRepository groupBotRepository;
    private static GroupPerkRepository groupPerkRepository;
    private static LevelRoleRepository levelRoleRepository;
    private static MarriageRepository marriageRepository;
    private static MarriageRequestRepository marriageRequestRepository;
    private static MemberEmojiRepository memberEmojiRepository;
    private static MemberPunishmentRepository memberPunishmentRepository;
    private static MessageTranscriptionRepository messageTranscriptionRepository;
    private static OficinaGroupRepository oficinaGroupRepository;
    private static ReminderRepository reminderRepository;
    private static SupportTicketRepository supportTicketRepository;
    private static TempBanRepository tempBanRepository;
    private static UserEconomyRepository userEconomyRepository;
    private static UserNameUpdateRepository userNameUpdateRepository;
    private static UserPreferenceRepository userPreferenceRepository;
    private static UserRepository userRepository;
    private static UserSubscriptionRepository userSubscriptionRepository;
    private static UserXPRepository userXPRepository;

    private Repositories() {}

    public static AppUserBanRepository getAppUserBanRepository() {
        if (appUserBanRepository == null) appUserBanRepository = new AppUserBanRepository(getDSLContext());
        return appUserBanRepository;
    }

    public static AutomodActionRepository getAutomodActionRepository() {
        if (automodActionRepository == null) automodActionRepository = new AutomodActionRepository(getDSLContext());
        return automodActionRepository;
    }

    public static BankTransactionRepository getBankTransactionRepository() {
        if (bankTransactionRepository == null) bankTransactionRepository = new BankTransactionRepository(getDSLContext());
        return bankTransactionRepository;
    }

    public static BetGameRepository getBetGameRepository() {
        if (gameRepository == null) gameRepository = new BetGameRepository(getDSLContext());
        return gameRepository;
    }

    public static BirthdayRepository getBirthdayRepository() {
        if (birthdayRepository == null) birthdayRepository = new BirthdayRepository(getDSLContext());
        return birthdayRepository;
    }

    public static BlockedWordRepository getBlockedWordRepository() {
        if (blockedWordRepository == null) blockedWordRepository = new BlockedWordRepository(getDSLContext());
        return blockedWordRepository;
    }

    public static ColorRoleStateRepository getColorRoleStateRepository() {
        if (colorRoleStateRepository == null) colorRoleStateRepository = new ColorRoleStateRepository(getDSLContext());
        return colorRoleStateRepository;
    }

    public static CommandHistoryRepository getCommandHistoryRepository() {
        if (commandHistoryRepository == null) commandHistoryRepository = new CommandHistoryRepository(getDSLContext());
        return commandHistoryRepository;
    }

    public static CustomUserinfoRepository getCustomUserinfoRepository() {
        if (customUserinfoRepository == null) customUserinfoRepository = new CustomUserinfoRepository(getDSLContext());
        return customUserinfoRepository;
    }

    public static DiscordMessageRepository getDiscordMessageRepository() {
        if (discordMessageRepository == null) discordMessageRepository = new DiscordMessageRepository(getDSLContext());
        return discordMessageRepository;
    }

    public static DiscordMessageUpdateRepository getDiscordMessageUpdateRepository() {
        if (discordMessageUpdateRepository == null) discordMessageUpdateRepository = new DiscordMessageUpdateRepository(getDSLContext());
        return discordMessageUpdateRepository;
    }

    public static EntityPolicyRepository getEntityPolicyRepository() {
        if (entityPolicyRepository == null) entityPolicyRepository = new EntityPolicyRepository(getDSLContext());
        return entityPolicyRepository;
    }

    public static FormerMemberRoleRepository getFormerMemberRoleRepository() {
        if (formerMemberRoleRepository == null) formerMemberRoleRepository = new FormerMemberRoleRepository(getDSLContext());
        return formerMemberRoleRepository;
    }

    public static GameParticipantRepository getGameParticipantRepository() {
        if (gameParticipantRepository == null) gameParticipantRepository = new GameParticipantRepository(getDSLContext());
        return gameParticipantRepository;
    }

    public static GroupBotRepository getGroupBotRepository() {
        if (groupBotRepository == null) groupBotRepository = new GroupBotRepository(getDSLContext());
        return groupBotRepository;
    }

    public static GroupPerkRepository getGroupPerkRepository() {
        if (groupPerkRepository == null) groupPerkRepository = new GroupPerkRepository(getDSLContext());
        return groupPerkRepository;
    }

    public static LevelRoleRepository getLevelRoleRepository() {
        if (levelRoleRepository == null) levelRoleRepository = new LevelRoleRepository(getDSLContext());
        return levelRoleRepository;
    }

    public static MarriageRepository getMarriageRepository() {
        if (marriageRepository == null) marriageRepository = new MarriageRepository(getDSLContext());
        return marriageRepository;
    }

    public static MarriageRequestRepository getMarriageRequestRepository() {
        if (marriageRequestRepository == null) marriageRequestRepository = new MarriageRequestRepository(getDSLContext());
        return marriageRequestRepository;
    }

    public static MemberEmojiRepository getMemberEmojiRepository() {
        if (memberEmojiRepository == null) memberEmojiRepository = new MemberEmojiRepository(getDSLContext());
        return memberEmojiRepository;
    }

    public static MemberPunishmentRepository getMemberPunishmentRepository() {
        if (memberPunishmentRepository == null) memberPunishmentRepository = new MemberPunishmentRepository(getDSLContext());
        return memberPunishmentRepository;
    }

    public static MessageTranscriptionRepository getMessageTranscriptionRepository() {
        if (messageTranscriptionRepository == null) messageTranscriptionRepository = new MessageTranscriptionRepository(getDSLContext());
        return messageTranscriptionRepository;
    }

    public static OficinaGroupRepository getOficinaGroupRepository() {
        if (oficinaGroupRepository == null) oficinaGroupRepository = new OficinaGroupRepository(getDSLContext());
        return oficinaGroupRepository;
    }

    public static ReminderRepository getReminderRepository() {
        if (reminderRepository == null) reminderRepository = new ReminderRepository(getDSLContext());
        return reminderRepository;
    }

    public static SupportTicketRepository getSupportTicketRepository() {
        if (supportTicketRepository == null) supportTicketRepository = new SupportTicketRepository(getDSLContext());
        return supportTicketRepository;
    }

    public static TempBanRepository getTempBanRepository() {
        if (tempBanRepository == null) tempBanRepository = new TempBanRepository(getDSLContext());
        return tempBanRepository;
    }

    public static UserEconomyRepository getUserEconomyRepository() {
        if (userEconomyRepository == null) userEconomyRepository = new UserEconomyRepository(getDSLContext());
        return userEconomyRepository;
    }

    public static UserNameUpdateRepository getUserNameUpdateRepository() {
        if (userNameUpdateRepository == null) userNameUpdateRepository = new UserNameUpdateRepository(getDSLContext());
        return userNameUpdateRepository;
    }

    public static UserPreferenceRepository getUserPreferenceRepository() {
        if (userPreferenceRepository == null) userPreferenceRepository = new UserPreferenceRepository(getDSLContext());
        return userPreferenceRepository;
    }

    public static UserRepository getUserRepository() {
        if (userRepository == null) userRepository = new UserRepository(getDSLContext());
        return userRepository;
    }

    public static UserSubscriptionRepository getUserSubscriptionRepository() {
        if (userSubscriptionRepository == null) userSubscriptionRepository = new UserSubscriptionRepository(getDSLContext());
        return userSubscriptionRepository;
    }

    public static UserXPRepository getUserXPRepository() {
        if (userXPRepository == null) userXPRepository = new UserXPRepository(getDSLContext());
        return userXPRepository;
    }

    private static DSLContext getDSLContext() {
        return DB.getContext();
    }
}