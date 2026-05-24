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
public class BotChangelogRoleHandler extends ListenerAdapter {

    @Override
    @SuppressWarnings("DataFlowIssue")
    public void onButtonInteraction(ButtonInteractionEvent e) {
        String id = e.getComponentId();
        if (!"bot-notification".equals(id)) return;

        Guild guild = e.getGuild();
        Role role = Roles.NOTIFY_BOT.role();

        if (role == null) {
            e.reply("Role not found 😟").setEphemeral(true).queue();
            return;
        }

        Member member = e.getMember();
        List<Role> roles = member.getRoles();

        if (roles.contains(role)) {
            guild.removeRoleFromMember(member, role).queue();
            e.replyFormat("Prontinho! Seu cargo foi removido.").setEphemeral(true).queue();
        } else {
            guild.addRoleToMember(member, role).queue();
            e.replyFormat("Tudo certo! Você recebeu o cargo %s.", role.getAsMention()).setEphemeral(true).queue();
        }
    }
}
