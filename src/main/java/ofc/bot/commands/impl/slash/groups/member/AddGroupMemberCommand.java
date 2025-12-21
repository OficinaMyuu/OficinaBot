package ofc.bot.commands.impl.slash.groups.member;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.GroupHelper;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "group member add")
public class AddGroupMemberCommand extends SlashSubcommand {
    private final OficinaGroupRepository grpRepo;

    public AddGroupMemberCommand(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long issuerId = ctx.getUserId();
        Member issuer = ctx.getIssuer();
        Member newMember = ctx.getOption("member", OptionMapping::getAsMember);
        Guild guild = ctx.getGuild();
        OficinaGroup group = grpRepo.findByOwnerId(issuerId);

        if (newMember == null)
            return Status.MEMBER_NOT_FOUND;

        if (group == null)
            return Status.YOU_DO_NOT_OWN_A_GROUP;

        if (newMember.getUser().isBot())
            return Status.CANNOT_ADD_BOTS_TO_GROUP;

        long groupRoleId = group.getRoleId();
        Role groupRole = guild.getRoleById(groupRoleId);

        // Also impossible
        if (groupRole == null)
            return Status.GROUP_ROLE_NOT_FOUND;

        if (newMember.getRoles().contains(groupRole))
            return Status.MEMBER_ALREADY_IN_THE_GROUP;

        boolean hasFreeAccess = group.hasFreeAccess();
        boolean hasFreeSlots = GroupHelper.hasFreeSlots(group);
        boolean targetHasFreeAccess = OficinaGroup.hasFreeAccess(newMember);
        int price = hasFreeAccess || targetHasFreeAccess || hasFreeSlots ? 0 : StoreItemType.GROUP_SLOT.getPrice();
        Button confirm = EntityContextFactory.createAddGroupMemberConfirm(group, newMember, price);
        MessageEmbed embed = EmbedFactory.embedGroupMemberAdd(issuer, group, newMember, price);
        return ctx.create()
                .setActionRows(confirm)
                .setEmbeds(embed)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Adiciona um membro ao seu grupo.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro que você deseja adicionar no seu grupo.", true)
        );
    }
}