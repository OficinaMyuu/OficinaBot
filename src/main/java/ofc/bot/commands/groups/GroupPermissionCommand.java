package ofc.bot.commands.groups;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.GroupPermission;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.sqlite.repository.EntityPolicyRepository;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.handlers.groups.permissions.GroupPermissionManager;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "group permission")
public class GroupPermissionCommand extends SlashSubcommand {
    private final GroupPermissionManager permissionManager;
    private final OficinaGroupRepository grpRepo;

    public GroupPermissionCommand(OficinaGroupRepository grpRepo, EntityPolicyRepository policyRepo) {
        this.permissionManager = new GroupPermissionManager(policyRepo);
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onSlashCommand(SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        Member issuer = ctx.getIssuer();
        GroupPermission perm = ctx.getSafeEnumOption("group-permission", GroupPermission.class);
        OficinaGroup group = grpRepo.findByOwnerId(userId);

        if (group == null)
            return Status.YOU_DO_NOT_OWN_A_GROUP;

        if (group.getTextChannel() == null)
            return Status.YOUR_GROUP_DOES_NOT_HAVE_TEXT_CHANNEL;

        boolean isGranted = permissionManager.isGranted(perm, group);
        if (isGranted)
            return Status.GROUP_PERMISSION_ALREADY_GRANTED;

        boolean isFree = group.hasFreeAccess();
        int price = isFree ? 0 : StoreItemType.GROUP_PERMISSION.getPrice();
        Button confirm = EntityContextFactory.createPermissionConfirm(group, perm, price);
        MessageEmbed embed = EmbedFactory.embedGroupPermissionAdd(issuer, group, perm, price);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRow(confirm)
                .send();
    }

    @Override
    protected void init() {
        setDesc("Compre permissões para o seu grupo.");
        setCooldown(3, TimeUnit.SECONDS);

        addOpt(OptionType.STRING, "group-permission", "A permissão a ser concedida no grupo.", (it) -> it.setRequired(true)
                .addChoices(getChoices()));
    }

    private List<Command.Choice> getChoices() {
        return GroupPermissionManager.findAll()
                .stream()
                .map(perm -> new Command.Choice(perm.getDisplay(), perm.name()))
                .toList();
    }
}