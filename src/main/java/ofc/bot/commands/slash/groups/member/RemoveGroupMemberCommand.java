package ofc.bot.commands.slash.groups.member;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "group member remove")
public class RemoveGroupMemberCommand extends SlashSubcommand {
    private final OficinaGroupRepository grpRepo;

    public RemoveGroupMemberCommand(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long ownerId = ctx.getUserId();
        Member issuer = ctx.getIssuer();
        Member member = ctx.getOption("member", OptionMapping::getAsMember);
        OficinaGroup group = grpRepo.findByOwnerId(ownerId);

        if (group == null)
            return Status.YOU_DO_NOT_OWN_A_GROUP;

        if (member == null)
            return Status.MEMBER_NOT_FOUND;

        if (member.equals(issuer))
            return Status.CANNOT_LEAVE_YOUR_OWN_GROUP;

        if (!hasRole(member, group.getRoleId()))
            return Status.MEMBER_NOT_IN_THE_GROUP;

        Button confirm = EntityContextFactory.createRemoveGroupMemberConfirm(group, member.getIdLong());
        MessageEmbed embed = EmbedFactory.embedGroupMemberRemove(issuer, group, member);
        return ctx.create(true)
                .setActionRow(confirm)
                .setEmbeds(embed)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Remove um membro do seu grupo.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a ser removido do grupo.", true)
        );
    }

    private boolean hasRole(Member member, long roleId) {
        return member.getRoles()
                .stream()
                .anyMatch(r -> r.getIdLong() == roleId);
    }
}
