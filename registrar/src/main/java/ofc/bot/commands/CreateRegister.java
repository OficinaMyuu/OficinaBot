package ofc.bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.database.models.RegisterData;
import ofc.bot.database.repositories.RegistersRepository;
import ofc.bot.exceptions.CommandPatternParsingException;
import ofc.bot.utils.BotUtil;
import ofc.bot.utils.Channels;
import ofc.bot.utils.CommandParser;
import ofc.bot.internal.BotData;
import ofc.bot.utils.Roles;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CreateRegister extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateRegister.class);
    private static final RegistersRepository repo = RegistersRepository.getInstance();

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        MessageChannel channel = e.getChannel();
        Message msg = e.getMessage();
        Member issuer = e.getMember();
        Guild guild = e.getGuild();
        String raw = msg.getContentRaw();
        String[] args = CommandParser.resolveArgs(raw);

        if (!raw.startsWith(BotData.PREFIX) || raw.startsWith("r!revoke")) return;

        if (issuer == null || !checkPermission(issuer)) return;

        BotUtil.delete(msg);
        try {
            RegisterAction register = createRegister(args[0]);
            List<Role> rolesAdd = register.getRolesToAdd();
            List<Role> rolesRemove = register.getRolesToRemove();
            long issuerId = issuer.getIdLong();

            if (register.age <= 0) {
                BotUtil.tempMessage("Idade inválida: `" + args[0] + "`.", channel, 10000);
                return;
            }

            CommandParser.findTarget(guild, args[1]).queue(target -> {
                long targetId = target.getIdLong();

                if (target.getUser().isBot()) {
                    BotUtil.tempMessage("Você não pode registrar um bot.", channel, 10000);
                    return;
                }

                if (targetId == issuerId) {
                    BotUtil.tempMessage("Você não pode registrar você mesmo.", channel, 10000);
                    return;
                }

                if (Roles.REGISTERED.isPresent(target)) {
                    BotUtil.tempMessage("Este membro já está registrado.", channel, 10000);
                    return;
                }

                if (Roles.VERIFYING.isPresent(target)) {
                    BotUtil.tempMessage("O usuário ainda está em verificação.", channel, 10000);
                    return;
                }

                guild.modifyMemberRoles(target, rolesAdd, rolesRemove).queue(s -> {
                    RegisterData newRegister = new RegisterData(register.gender, register.device, register.age, target, issuer);
                    logToChannel(target, issuer, rolesAdd);

                    LOGGER.info("@{} has successfully registered @{}", issuer.getUser().getName(), target.getUser().getName());

                    repo.save(newRegister);
                    deleteLastMessageByUser(channel, targetId);

                    BotUtil.tempMessage(target.getAsMention() + " registrado com sucesso!", channel, 10000);

                }, err -> {
                    BotUtil.tempMessage("Erro :/", channel, 5000);
                    LOGGER.error("Could not add roles to target {}", target.getId(), err);
                });

            }, err -> BotUtil.tempMessage("Membro não encontrado.", channel, 10000));

        } catch (CommandPatternParsingException err) {
            BotUtil.tempMessage("A sintaxe do comando está incorreta: `" + args[0] + "`", channel, 10000);
        } catch (DataAccessException err) {
            LOGGER.error("Could not insert row to the database", err);
        }
    }

    private boolean checkPermission(Member member) {
        return Roles.REGISTRAR.isPresent(member) || member.hasPermission(Permission.MANAGE_ROLES);
    }

    private RegisterAction createRegister(String arg) {
        int age = CommandParser.findAge(arg);
        Roles device = CommandParser.findDevice(arg);
        Roles gender = CommandParser.findGender(arg);

        return new RegisterAction(age, device, gender);
    }

    private void deleteLastMessageByUser(MessageChannel chan, long userId) {
        chan.getHistory().retrievePast(20).queue(msgs -> {
            for (Message msg : msgs) {
                if (msg.getAuthor().getIdLong() == userId) {
                    msg.delete().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                }
            }
        });
    }

    private void logToChannel(Member target, Member mod, List<Role> rolesAdded) {
        TextChannel chan = Channels.RL.channel(TextChannel.class);
        MessageEmbed embed = getLogEmbed(target, mod, rolesAdded.stream().map(Role::getAsMention).toList());

        if (chan == null) {
            LOGGER.warn("Log channel was not found at ID {}", Channels.RL.fetchId());
            return;
        }
        chan.sendMessageEmbeds(embed).queue();
    }

    private MessageEmbed getLogEmbed(Member target, Member mod, List<String> rolesMention) {
        EmbedBuilder builder = new EmbedBuilder();
        Guild guild = mod.getGuild();
        String targetName = target.getUser().getName();
        String modName = mod.getUser().getName();
        String avatarUrl = target.getUser().getAvatarUrl();
        String roles = String.join("\n", rolesMention);

        return builder.setColor(Color.GREEN)
                .setTitle("`" + targetName + "` foi registrado!")
                .setThumbnail(avatarUrl)
                .setDescription("Registrado por `" + modName + "`.")
                .addField("Cargos", roles, false)
                .setFooter(guild.getName() + "・ID: " + target.getId(), guild.getIconUrl())
                .build();
    }

    private record RegisterAction(
            int age,
            Roles device,
            Roles gender
    ) {
        public List<Role> getRolesToAdd() {
            List<Role> roles = new ArrayList<>(4);

            Role registeredRole = Roles.REGISTERED.getRole();
            Role deviceRole = device.getRole();
            Role genderRole = gender.getRole();
            List<Role> ageRoles = getAgeRoles();

            roles.add(registeredRole);
            roles.add(deviceRole);
            roles.add(genderRole);
            roles.addAll(ageRoles);

            return roles;
        }

        public List<Role> getRolesToRemove() {
            Role nonRegistered = Roles.NON_REGISTERED.getRole();

            return nonRegistered == null
                    ? List.of()
                    : List.of(nonRegistered);
        }

        private List<Role> getAgeRoles() {
            return Roles.getByAge(this.age)
                    .stream()
                    .map(Roles::getRole)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }
}