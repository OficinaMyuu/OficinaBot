package ofc.bot.listeners.discord.guilds.reactionroles;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.util.content.Roles;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;

@DiscordEventHandler
public class StudyRoleHandler extends ListenerAdapter {

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void onButtonInteraction(ButtonInteractionEvent e) {
        String id = e.getComponentId();
        if (!"enem".equals(id)) return;

        Guild guild = e.getGuild();
        Role study = Roles.STUDY.role();

        if (study == null) {
            e.reply("Role not found 😟").setEphemeral(true).queue();
            return;
        }

        Member member = e.getMember();
        List<Role> roles = member.getRoles();

        if (roles.contains(study)) {
            guild.removeRoleFromMember(member, study).queue();
            e.replyFormat("<:kaisadezoi:1057715180584304680> Prontinho! Seu cargo foi removido.").setEphemeral(true).queue();
        } else {
            guild.addRoleToMember(member, study).queue();
            e.replyFormat("<:Hiro:855653864693694494> Tudo certo! Você recebeu o cargo %s.", study.getAsMention()).setEphemeral(true).queue();
        }
    }
}