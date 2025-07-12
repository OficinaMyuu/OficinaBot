package ofc.bot.handlers.interactions.commands.slash;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import ofc.bot.Main;
import ofc.bot.commands.slash.*;
import ofc.bot.commands.slash.additionals.AdditionalRolesCommand;
import ofc.bot.commands.slash.bets.BetTicTacToeCommand;
import ofc.bot.commands.slash.birthday.BirthdayAddCommand;
import ofc.bot.commands.slash.birthday.BirthdayRemoveCommand;
import ofc.bot.commands.slash.birthday.BirthdaysCommand;
import ofc.bot.commands.slash.economy.*;
import ofc.bot.commands.slash.groups.*;
import ofc.bot.commands.slash.groups.channel.CreateGroupChannelCommand;
import ofc.bot.commands.slash.groups.member.AddGroupMemberCommand;
import ofc.bot.commands.slash.groups.member.RemoveGroupMemberCommand;
import ofc.bot.commands.slash.levels.LevelsCommand;
import ofc.bot.commands.slash.levels.LevelsRolesCommand;
import ofc.bot.commands.slash.levels.RankCommand;
import ofc.bot.commands.slash.moderation.*;
import ofc.bot.commands.slash.policies.AddPolicyCommand;
import ofc.bot.commands.slash.policies.RemovePolicyCommand;
import ofc.bot.commands.slash.relationships.DivorceCommand;
import ofc.bot.commands.slash.relationships.MarryCommand;
import ofc.bot.commands.slash.relationships.UpdateMarriageCreationCommand;
import ofc.bot.commands.slash.relationships.marriages.*;
import ofc.bot.commands.slash.reminders.*;
import ofc.bot.commands.slash.stafflist.RefreshStaffListMessageCommand;
import ofc.bot.commands.slash.stafflist.StaffListMessagesRegenerateCommand;
import ofc.bot.commands.slash.twitch.ListTwitchChannelsCommand;
import ofc.bot.commands.slash.twitch.SubscribeTwitchCommand;
import ofc.bot.commands.slash.twitch.UnsubscribeTwitchCommand;
import ofc.bot.commands.slash.userinfo.UserinfoCommand;
import ofc.bot.commands.slash.userinfo.custom.*;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.handlers.interactions.commands.slash.abstractions.ICommand;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.interactions.commands.slash.dummy.EmptySlashCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class CommandsInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsInitializer.class);

    /**
     * This method instantiates all slash commands and also
     * sends them to Discord.
     */
    public static void initializeSlashCommands() {
        SlashCommandsRegistryManager registry = SlashCommandsRegistryManager.getManager();
        var twitchSubRepo = Repositories.getTwitchSubscriptionRepository();
        var bckpRepo      = Repositories.getFormerMemberRoleRepository();
        var pnshRepo      = Repositories.getMemberPunishmentRepository();
        var mreqRepo      = Repositories.getMarriageRequestRepository();
        var csinfoRepo    = Repositories.getCustomUserinfoRepository();
        var msgRepo       = Repositories.getDiscordMessageRepository();
        var namesRepo     = Repositories.getUserNameUpdateRepository();
        var modActRepo    = Repositories.getAutomodActionRepository();
        var policyRepo    = Repositories.getEntityPolicyRepository();
        var grpRepo       = Repositories.getOficinaGroupRepository();
        var emjRepo       = Repositories.getMemberEmojiRepository();
        var ecoRepo       = Repositories.getUserEconomyRepository();
        var lvlRoleRepo   = Repositories.getLevelRoleRepository();
        var bdayRepo      = Repositories.getBirthdayRepository();
        var grpBotRepo    = Repositories.getGroupBotRepository();
        var marrRepo      = Repositories.getMarriageRepository();
        var remRepo       = Repositories.getReminderRepository();
        var tmpBanRepo    = Repositories.getTempBanRepository();
        var xpRepo        = Repositories.getUserXPRepository();
        var userRepo      = Repositories.getUserRepository();

        // Additionals
        SlashCommand additionals = new EmptySlashCommand("additionals", "Gerencia recursos adicionais/misc do bot.", Permission.MANAGE_SERVER)
                .addSubcommand(new AdditionalRolesCommand());

        SlashCommand bets = new EmptySlashCommand("bets", "Aposte seu dinheiro.")
                .addSubcommand(new BetTicTacToeCommand(ecoRepo));

        // Birhday
        SlashCommand birthday = new EmptySlashCommand("birthday", "Gerencia os aniversários.", Permission.MANAGE_SERVER)
                .addSubcommand(new BirthdayAddCommand(bdayRepo, policyRepo))
                .addSubcommand(new BirthdayRemoveCommand(bdayRepo));

        // Policies
        SlashCommand policies = new EmptySlashCommand("policies", "Gerencia as regras dos módulos do bot.", Permission.MANAGE_SERVER)
                .addSubcommand(new AddPolicyCommand(policyRepo))
                .addSubcommand(new RemovePolicyCommand(policyRepo));

        // Marriage
        SlashCommand marriage = new EmptySlashCommand("marriage", "Gerencia os seus casamentos.")
                .addSubcommand(new MarriageAcceptCommand(mreqRepo, marrRepo, ecoRepo))
                .addSubcommand(new CancelProposalCommand(mreqRepo))
                .addSubcommand(new ProposalsListCommand(mreqRepo))
                .addSubcommand(new MarriageRejectCommand(mreqRepo));

        // Twitch
        SlashCommand twitch = new EmptySlashCommand("twitch", "Gerencia as atividades da Twitch.")
                .addSubcommand(new ListTwitchChannelsCommand())
                .addSubcommand(new SubscribeTwitchCommand(twitchSubRepo))
                .addSubcommand(new UnsubscribeTwitchCommand(twitchSubRepo));

        // Custom Userinfo
        SlashCommand customizeUserinfo = new EmptySlashCommand("customize", "Customize o seu userinfo.", Permission.MANAGE_SERVER)
                .addSubcommand(new ResetUserinfoCommand(csinfoRepo))
                .addSubcommand(new SetUserinfoColorCommand(csinfoRepo))
                .addSubcommand(new SetDescriptionCommand(csinfoRepo))
                .addSubcommand(new SetUserinfoFooterCommand(csinfoRepo));

        // Groups
        SlashCommand group = new EmptySlashCommand("group", "Tenha o controle de tudo sobre o seu grupo.")
                .addGroups(
                        new SubcommandGroup("channel", "Gerencie os canais do seu grupo.")
                                .addSubcommand(new CreateGroupChannelCommand(grpRepo)),

                        new SubcommandGroup("member", "Gerencie os membros do seu grupo.")
                                .addSubcommand(new AddGroupMemberCommand(grpRepo))
                                .addSubcommand(new RemoveGroupMemberCommand(grpRepo))
                )
                .addSubcommand(new CreateGroupCommand(grpRepo))
                .addSubcommand(new GroupBotsCommand(grpBotRepo, grpRepo))
                .addSubcommand(new GroupInfoCommand(grpRepo))
                .addSubcommand(new GroupPayInvoiceCommand(grpRepo))
                .addSubcommand(new GroupPermissionCommand(grpRepo, policyRepo))
                .addSubcommand(new GroupPinsCommand(grpRepo))
                .addSubcommand(new HelpGroupCommand())
                .addSubcommand(new LeaveGroupCommand(grpRepo))
                .addSubcommand(new ModifyGroupCommand(grpRepo));

        // Reminders
        SlashCommand remind = new EmptySlashCommand("remind", "Crie lembretes para organizar sua rotina.")
                .addSubcommand(new CreateAtReminderCommand(remRepo))
                .addSubcommand(new CreateCronReminderCommand(remRepo))
                .addSubcommand(new CreateFixedReminderCommand(remRepo))
                .addSubcommand(new CreatePeriodicReminderCommand(remRepo))
                .addSubcommand(new ListRemindersCommand());

        // Administration
        registry.register(new DisconnectAllCommand());
        registry.register(new MoveAllCommand());

        // Birthdays
        registry.register(new BirthdaysCommand(bdayRepo));

        // Economy
        registry.register(new BalanceCommand(ecoRepo));
        registry.register(new DailyCommand(ecoRepo));
        registry.register(new DepositCommand(ecoRepo));
        registry.register(new LeaderboardCommand(ecoRepo));
        registry.register(new PayCommand(ecoRepo));
        registry.register(new RobCommand(ecoRepo));
        registry.register(new SetMoneyCommand(ecoRepo));
        registry.register(new TransactionsCommand());
        registry.register(new UpdateMoneyCommand(ecoRepo));
        registry.register(new WithdrawCommand(ecoRepo));
        registry.register(new WorkCommand(ecoRepo));

        // Levels
        registry.register(new LevelsCommand(xpRepo));
        registry.register(new LevelsRolesCommand(lvlRoleRepo));
        registry.register(new RankCommand(xpRepo, lvlRoleRepo));

        // Moderation
        registry.register(new BanCommand(tmpBanRepo));
        registry.register(new InfractionsCommand());
        registry.register(new KickCommand());
        registry.register(new MuteCommand());
        registry.register(new UnbanCommand());
        registry.register(new UnmuteCommand());
        registry.register(new WarnCommand(pnshRepo, modActRepo));

        // Relationships
        registry.register(new DivorceCommand(marrRepo));
        registry.register(new MarryCommand(mreqRepo, ecoRepo, marrRepo, userRepo));
        registry.register(new UpdateMarriageCreationCommand(marrRepo));

        // Staff List
        registry.register(new StaffListMessagesRegenerateCommand());
        registry.register(new RefreshStaffListMessageCommand(policyRepo));

        // Userinfo
        registry.register(new UserinfoCommand(csinfoRepo, emjRepo, ecoRepo, marrRepo, grpRepo));

        // Generic
        registry.register(new AvatarCommand());
        registry.register(new BackupMemberRolesCommand(bckpRepo));
        registry.register(new BotStatusCommand(lvlRoleRepo));
        registry.register(new ClearMessagesCommand());
        registry.register(new GuildInfoCommand());
        registry.register(new GuildLogoCommand());
        registry.register(new IPLookupCommand());
        registry.register(new MovieInstructionsCommand());
        registry.register(new NamesHistoryCommand(namesRepo));
        registry.register(new OpenTicketCommand());
        registry.register(new RoleAmongUsCommand());
        registry.register(new RoleInfoCommand());
        registry.register(new RoleMembersCommand());
        registry.register(new ToggleEventsCommand());
        registry.register(new ViewTicketCommand(msgRepo));

        // Compound Commands
        registry.register(additionals);
        registry.register(bets);
        registry.register(birthday);
        registry.register(policies);
        registry.register(marriage);
        registry.register(remind);
        registry.register(group);
        registry.register(twitch);
        registry.register(customizeUserinfo);

        // Send them to Discord and clear the temporary cache
        pushCommands(registry.getAll());
        registry.clearTemp();
    }

    private static void pushCommands(List<SlashCommand> cmds) {
        JDA api = Main.getApi();
        List<SlashCommandData> slashCmds = cmds.stream()
                .map(ICommand::buildSlash)
                .toList();

        api.updateCommands().addCommands(slashCmds).queue(
                CommandsInitializer::printTree,
                (err) -> LOGGER.error("Failed to create slash commands", err)
        );
    }

    private static void printTree(List<Command> commands) {
        int count = commands.size();
        for (int i = 0; i < count; i++) {
            boolean isLast = i == count - 1;
            Command command = commands.get(i);
            List<Command.Subcommand> subcommands = command.getSubcommands();
            List<Command.SubcommandGroup> groups = command.getSubcommandGroups();
            String prefix = isLast ? "└───" : "├───";

            LOGGER.info("{} {}", prefix, command.getName());

            if (!subcommands.isEmpty()) {
                printSubcommands(subcommands, false);
            }

            if (!groups.isEmpty()) {
                printSubcommandGroups(groups);
            }

        }
    }

    private static void printSubcommands(List<Command.Subcommand> subcommands, boolean isInGroup) {
        int count = subcommands.size();
        for (int i = 0; i < count; i++) {
            boolean isLast = i == count - 1;
            Command.Subcommand cmd = subcommands.get(i);
            String indent = isInGroup ? "   │" : "   ";
            String prefix = String.format("%s%s", indent, (isLast ? "└───" : "├───"));

            LOGGER.info("│{} {}", prefix, cmd.getName());
        }
    }

    private static void printSubcommandGroups(List<Command.SubcommandGroup> groups) {
        int count = groups.size();
        for (int i = 0; i < count; i++) {
            boolean isLast = i == count - 1;
            Command.SubcommandGroup group = groups.get(i);
            String prefix = isLast ? "└───" : "├───";

            LOGGER.info("│{}{} (group)", prefix, group.getName());

            if (!group.getSubcommands().isEmpty()) {
                printSubcommands(group.getSubcommands(), true);
            }
        }
    }
}