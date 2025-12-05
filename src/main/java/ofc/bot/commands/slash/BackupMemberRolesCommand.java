package ofc.bot.commands.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.FormerMemberRole;
import ofc.bot.domain.sqlite.repository.FormerMemberRoleRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

@DiscordCommand(name = "backup", permissions = Permission.MANAGE_SERVER)
public class BackupMemberRolesCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupMemberRolesCommand.class);
    private final FormerMemberRoleRepository bckpRepo;

    public BackupMemberRolesCommand(FormerMemberRoleRepository bckpRepo) {
        this.bckpRepo = bckpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        ctx.ack();
        Member target = ctx.getOption("user", OptionMapping::getAsMember);
        Guild guild = ctx.getGuild();
        boolean keep = ctx.getOption("keep", false, OptionMapping::getAsBoolean);

        if (target == null)
            return Status.MEMBER_NOT_FOUND;

        long targetId = target.getIdLong();
        long guildId = guild.getIdLong();
        List<FormerMemberRole> rolesRecord = bckpRepo.findByUserAndGuildId(targetId, guildId);
        List<Role> roles = resolveRoles(guild, rolesRecord);

        if (roles.isEmpty())
            return Status.ROLES_NOT_FOUND_TO_BACKUP;

        guild.modifyMemberRoles(target, roles, null).queue((s) -> {
            String name = target.getUser().getEffectiveName();
            int amount = roles.size();

            ctx.reply(Status.ROLES_SUCCESSFULLY_BACKED_UP.args(amount, name));

            if (!keep)
                bckpRepo.deleteByUserAndGuildId(guildId, targetId);
        }, (e) -> {
            LOGGER.error("Could not backup roles of member '{}' in guild '{}'", targetId, guildId);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Recupere todos os cargos de um membro que saiu anteriormente (ou não).";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O alvo a devolver os cargos.", true),
                new OptionData(OptionType.BOOLEAN, "keep", "Se devemos manter os cargos do backup assim que eles forem devolvidos ao membro (Padrão: False).")
        );
    }

    private List<Role> resolveRoles(Guild guild, List<FormerMemberRole> roles) {
        Member bot = guild.getSelfMember();
        return roles.stream()
                .map(FormerMemberRole::getRoleId)
                .map(guild::getRoleById)
                .filter(Objects::nonNull)
                // Ensure the bot will never attempt to give the user a role
                // they don't have permission to.
                .filter(bot::canInteract)
                .filter(r -> !r.isManaged())
                .toList();
    }
}