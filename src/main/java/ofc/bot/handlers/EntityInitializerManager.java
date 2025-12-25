package ofc.bot.handlers;

import net.dv8tion.jda.api.JDA;
import ofc.bot.Main;
import ofc.bot.commands.impl.slash.colors.AddColorRoleCommand;
import ofc.bot.commands.impl.slash.colors.RemoveColorRoleCommand;
import ofc.bot.commands.impl.slash.groups.LeaveGroupCommand;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.handlers.cache.PolicyService;
import ofc.bot.handlers.interactions.InteractionMemoryManager;
import ofc.bot.handlers.interactions.buttons.ButtonInteractionGateway;
import ofc.bot.handlers.interactions.commands.SlashCommandsGateway;
import ofc.bot.handlers.interactions.commands.slash.CommandsInitializer;
import ofc.bot.handlers.interactions.modals.ModalInteractionGateway;
import ofc.bot.jobs.*;
import ofc.bot.jobs.groups.GroupsInvoiceHandler;
import ofc.bot.jobs.groups.LateGroupsChecker;
import ofc.bot.jobs.income.VoiceChatMoneyHandler;
import ofc.bot.jobs.income.VoiceXPHandler;
import ofc.bot.jobs.roles.ExpiredBackupsRemover;
import ofc.bot.jobs.weekdays.SadMonday;
import ofc.bot.jobs.weekdays.SadSunday;
import ofc.bot.listeners.discord.economy.ChatMoneyHandler;
import ofc.bot.listeners.discord.guilds.BlockDumbCommands;
import ofc.bot.listeners.discord.guilds.UnbanTempBanCleaner;
import ofc.bot.listeners.discord.guilds.members.MemberJoinUpsert;
import ofc.bot.listeners.discord.guilds.messages.*;
import ofc.bot.listeners.discord.guilds.reactionroles.BotChangelogRoleHandler;
import ofc.bot.listeners.discord.guilds.reactionroles.StudyRoleHandler;
import ofc.bot.listeners.discord.guilds.roles.MemberRolesBackup;
import ofc.bot.listeners.discord.guilds.voice.solo.SoloChannelsHandler;
import ofc.bot.listeners.discord.interactions.GenericInteractionLocaleUpsert;
import ofc.bot.listeners.discord.interactions.autocomplete.*;
import ofc.bot.listeners.discord.interactions.buttons.WorkReminderHandler;
import ofc.bot.listeners.discord.interactions.buttons.bets.TicTacToeAcceptHandler;
import ofc.bot.listeners.discord.interactions.buttons.groups.*;
import ofc.bot.listeners.discord.interactions.buttons.pagination.*;
import ofc.bot.listeners.discord.interactions.buttons.pagination.infractions.DeleteInfraction;
import ofc.bot.listeners.discord.interactions.buttons.pagination.infractions.InfractionsPageUpdate;
import ofc.bot.listeners.discord.interactions.buttons.pagination.reminders.DeleteReminder;
import ofc.bot.listeners.discord.interactions.buttons.pagination.reminders.RemindersPageUpdate;
import ofc.bot.listeners.discord.interactions.buttons.shop.ColorRolePurchaseHandler;
import ofc.bot.listeners.discord.interactions.buttons.shop.ColorRoleRemoveHandler;
import ofc.bot.listeners.discord.interactions.buttons.tickets.CloseTicketHandler;
import ofc.bot.listeners.discord.interactions.dm.DirectMessageReceived;
import ofc.bot.listeners.discord.interactions.menus.ChoosableRolesListener;
import ofc.bot.listeners.discord.interactions.modals.ChoosableRolesHandler;
import ofc.bot.listeners.discord.interactions.modals.tickets.TicketClosureHandler;
import ofc.bot.listeners.discord.interactions.modals.tickets.TicketCreationHandler;
import ofc.bot.listeners.discord.logs.VoiceActivity;
import ofc.bot.listeners.discord.logs.messages.*;
import ofc.bot.listeners.discord.logs.moderation.LogTimeout;
import ofc.bot.listeners.discord.logs.moderation.automod.AutoModLogger;
import ofc.bot.listeners.discord.logs.names.MemberNickUpdateLogger;
import ofc.bot.listeners.discord.logs.names.UserGlobalNameUpdateLogger;
import ofc.bot.listeners.discord.logs.names.UserNameUpdateLogger;
import ofc.bot.listeners.discord.moderation.AutoModerator;
import ofc.bot.util.GroupHelper;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an utility class to register/initialize commands,
 * listeners, jobs, services, etc.
 * <p>
 * For {@link ofc.bot.util.content.annotations.jobs.CronJob CronJob}
 * classes, they are instantiated through the default constructor,
 * as {@code new Class()}.
 */
public final class EntityInitializerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityInitializerManager.class);

    /**
     * This method is just a way to streamline the process of instantiating
     * Slash Commands.
     * <p>
     * The operation is delegated to the default initializer, at
     * {@link CommandsInitializer#initializeSlashCommands()}.
     */
    public static void registerSlashCommands() {
        CommandsInitializer.initializeSlashCommands();
    }

    public static void initializeCronJobs() {
        try {
            SchedulerRegistryManager.initializeSchedulers(
                    new ExpiredBackupsRemover(),
                    new SadMonday(),
                    new SadSunday(),
                    new BirthdayReminder(),
                    new ColorRoleRemotionHandler(),
                    new ExpiredBanHandler(),
                    new HappyNewYearAnnouncement(),
                    new QueryCountPrinter(),
                    new RemindersHandler(),
                    new VoiceHeartbeatCounter(),

                    // Voice Income
                    new VoiceChatMoneyHandler(),
                    new VoiceXPHandler(),

                    // Groups
                    new GroupsInvoiceHandler(),
                    new LateGroupsChecker(),

                    // Nicks
                    new NickTimeUpdate()
            );
            SchedulerRegistryManager.start();
        } catch (SchedulerException e) {
            LOGGER.error("Could not initialize schedulers", e);
        }
    }

    public static void initServices() {
        var policyRepo  = Repositories.getEntityPolicyRepository();
        var grpPerkRepo = Repositories.getGroupPerkRepository();

        PolicyService.setPolicyRepo(policyRepo);
        GroupHelper.setRepositories(grpPerkRepo);
    }

    public static void registerComposedInteractions() {
        var colorStateRepo = Repositories.getColorRoleStateRepository();
        var betUsersRepo = Repositories.getGameParticipantRepository();
        var pnshRepo = Repositories.getMemberPunishmentRepository();
        var msgVrsRepo = Repositories.getMessageVersionRepository();
        var mreqRepo = Repositories.getMarriageRequestRepository();
        var namesRepo = Repositories.getUserNameUpdateRepository();
        var ticketRepo = Repositories.getSupportTicketRepository();
        var policyRepo = Repositories.getEntityPolicyRepository();
        var appBanRepo = Repositories.getAppUserBanRepository();
        var grpRepo = Repositories.getOficinaGroupRepository();
        var ecoRepo = Repositories.getUserEconomyRepository();
        var bdayRepo = Repositories.getBirthdayRepository();
        var remRepo = Repositories.getReminderRepository();
        var betRepo = Repositories.getBetGameRepository();
        var xpRepo = Repositories.getUserXPRepository();

        InteractionMemoryManager.getManager().registerListeners(
                // Infractions
                new DeleteInfraction(pnshRepo),
                new InfractionsPageUpdate(),

                // Reminders
                new DeleteReminder(remRepo),
                new RemindersPageUpdate(),

                // Pagination
                new BirthdayPageUpdate(bdayRepo),
                new LeaderboardOffsetUpdate(),
                new LevelsPageUpdate(xpRepo),
                new NamesPageUpdate(namesRepo),
                new ProposalListPagination(mreqRepo),
                new TicketsPagination(msgVrsRepo),

                // Shop
                new ColorRolePurchaseHandler(colorStateRepo),
                new ColorRoleRemoveHandler(colorStateRepo),

                // Groups' commands confirmation handlers
                new GroupBotAddHandler(),
                new GroupChannelCreationHandler(grpRepo),
                new GroupCreationHandler(grpRepo),
                new GroupInvoicePaymentHandler(grpRepo),
                new GroupMemberAddHandler(),
                new GroupMemberRemoveHandler(),
                new GroupPermissionAddHandler(policyRepo),
                new GroupPinsHandler(),
                new GroupUpdateHandler(grpRepo),

                // Bets
                new TicTacToeAcceptHandler(ecoRepo, betRepo, betUsersRepo, appBanRepo),

                // Tickets
                new TicketCreationHandler(ticketRepo),
                new TicketClosureHandler(ticketRepo),

                // Generic
                new ChoosableRolesHandler()
        );
    }

    public static void registerListeners() {
        registerDiscordListeners();
    }

    private static void registerDiscordListeners() {
        JDA api = Main.getApi();
        var msgTrscptRepo = Repositories.getMessageTranscriptionRepository();
        var colorStateRepo = Repositories.getColorRoleStateRepository();
        var colorItemRepo = Repositories.getColorRoleItemRepository();
        var rolesRepo = Repositories.getFormerMemberRoleRepository();
        var pnshRepo = Repositories.getMemberPunishmentRepository();
        var usprefRepo = Repositories.getUserPreferenceRepository();
        var mentionLogRepo = Repositories.getMentionLogRepository();
        var blckWordsRepo = Repositories.getBlockedWordRepository();
        var msgVrsRepo = Repositories.getMessageVersionRepository();
        var namesRepo = Repositories.getUserNameUpdateRepository();
        var modActRepo = Repositories.getAutomodActionRepository();
        var ticketRepo = Repositories.getSupportTicketRepository();
        var cmdRepo = Repositories.getCommandHistoryRepository();
        var appBanRepo = Repositories.getAppUserBanRepository();
        var grpRepo = Repositories.getOficinaGroupRepository();
        var ecoRepo = Repositories.getUserEconomyRepository();
        var grpBotRepo = Repositories.getGroupBotRepository();
        var tmpBanRepo = Repositories.getTempBanRepository();
        var xpRepo = Repositories.getUserXPRepository();
        var userRepo = Repositories.getUserRepository();

        api.addEventListener(
                new AddColorRoleCommand.ColorRoleListAutocompletionHandler(colorItemRepo),
                new AutoModerator(blckWordsRepo, pnshRepo, modActRepo, ticketRepo),
                new AutoModLogger(),
                new BlockDumbCommands(),
                new BotChangelogRoleHandler(),
                new ButtonInteractionGateway(appBanRepo),
                new ChatMoneyHandler(ecoRepo),
                new ChoosableRolesListener(),
                new CloseTicketHandler(),
                new DirectMessageReceived(),
                new ErikPingReactionHelper(),
                new GenericInteractionLocaleUpsert(usprefRepo),
                new GroupBotAutocompletion(grpBotRepo),
                new InfractionsAutocompletion(pnshRepo),
                new LeaveGroupCommand.FakePISuggester(),
                new LogTimeout(),
                new LorittaDailySpamBlocker(),
                new MentionLoggerHandler(mentionLogRepo),
                new MemberJoinUpsert(),
                new MemberNickUpdateLogger(namesRepo, userRepo),
                new MemberRolesBackup(rolesRepo, xpRepo),
                new ModalInteractionGateway(),
                new MessageBulkDeleteLogger(msgVrsRepo),
                new MessageCreatedLogger(msgVrsRepo),
                new MessageDeletedLogger(msgVrsRepo),
                new MessageReferenceIndicator(),
                new MessageTranscriptionsHandler(msgTrscptRepo, appBanRepo),
                new MessageUpdatedLogger(msgVrsRepo),
                new OficinaGroupAutocompletion(grpRepo),
                new OutageCommandsDisclaimer(),
                new RemoveColorRoleCommand.ColorRoleAutocompletionHandler(colorStateRepo),
                new ResourceAutocompletion(userRepo),
                new SlashCommandsGateway(cmdRepo, appBanRepo),
                new SoloChannelsHandler(),
                new SteamScamBlocker(),
                new StudyRoleHandler(),
                new UnbanTempBanCleaner(tmpBanRepo),
                new UserGlobalNameUpdateLogger(namesRepo, userRepo),
                new UserNameUpdateLogger(namesRepo, userRepo),
                new UsersXPHandler(),
                new VoiceActivity(),
                new VoiceDisconnector(),
                new WorkReminderHandler()
        );
    }
}