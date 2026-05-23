package ofc.bot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.utils.BotUtil;
import ofc.bot.utils.CommandParser;
import ofc.bot.utils.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RevokeRegister extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevokeRegister.class);

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();
        Member issuer = e.getMember();
        MessageChannel channel = e.getChannel();
        Guild guild = e.getGuild();
        String content = msg.getContentRaw();
        String[] args = content.split(" ");

        if (!content.startsWith("r!revoke")) return;

        if (issuer == null || !issuer.hasPermission(Permission.MANAGE_SERVER)) return;

        BotUtil.delete(msg);
        CommandParser.findTarget(guild, args[1]).queue(target -> {
            long issuerId = issuer.getIdLong();
            long targetId = target.getIdLong();

            if (target.getUser().isBot()) {
                BotUtil.tempMessage("Você não pode remover o registro de um bot.", channel, 10000);
                return;
            }

            if (targetId == issuerId) {
                BotUtil.tempMessage("Você não pode remover o seu próprio registro.", channel, 10000);
                return;
            }

            if (!Roles.isRegistered(target)) {
                BotUtil.tempMessage("Este membro não está registrado.", channel, 10000);
                return;
            }

            if (Roles.VERIFYING.isPresent(target)) {
                BotUtil.tempMessage("O usuário ainda está em verificação.", channel, 10000);
                return;
            }

            List<Role> rolesAdd = getRolesToAdd();
            List<Role> rolesRemove = getRolesToRemove();

            guild.modifyMemberRoles(target, rolesAdd, rolesRemove).queue(s -> {
                BotUtil.tempMessage("Registro de " + target.getAsMention() + " removido com sucesso!", channel, 10000);
            }, err -> {
                BotUtil.tempMessage("Erro :/", channel, 5000);
                LOGGER.error("Could not add roles to target {}", target.getId(), err);
            });
        }, err -> BotUtil.tempMessage("Membro não encontrado.", channel, 10000));
    }

    private List<Role> getRolesToAdd() {
        return Roles.getNonRegisteredRoles()
                .stream()
                .map(Roles::getRole)
                .toList();
    }

    private List<Role> getRolesToRemove() {
        return Roles.getRegisteredRoles()
                .stream()
                .map(Roles::getRole)
                .toList();
    }
}