package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.domain.entity.RegisterData;
import ofc.bot.domain.sqlite.repository.RegisterRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.registration.RegistrationAction;
import ofc.bot.handlers.registration.RegistrationDevice;
import ofc.bot.handlers.registration.RegistrationGender;
import ofc.bot.handlers.registration.RegistrationRole;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "register")
public class CreateRegisterCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateRegisterCommand.class);
    private static final String REGISTER_LOG_CHANNEL_KEY = "channels.registry.log";
    private static final int TEMP_RESPONSE_SECONDS = 10;
    private final RegisterRepository registerRepository;

    public CreateRegisterCommand(@NotNull RegisterRepository registerRepository) {
        this.registerRepository = registerRepository;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member issuer = ctx.getIssuer();

        if (!checkPermission(issuer)) {
            return Status.YOU_CANNOT_RUN_THIS_COMMAND;
        }

        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        RegistrationGender gender = ctx.getSafeEnumOption("gender", RegistrationGender.class);
        RegistrationDevice device = ctx.getSafeEnumOption("device", RegistrationDevice.class);
        int age = ctx.getSafeOption("age", OptionMapping::getAsInt);
        RegistrationAction register = new RegistrationAction(gender, device, age);

        if (target == null) {
            return Status.MEMBER_NOT_FOUND;
        }

        if (!register.hasValidAge()) {
            return replyTemporary(ctx, "Idade inv\u00e1lida: `" + age + "`.");
        }

        if (target.getUser().isBot()) {
            return replyTemporary(ctx, "Voc\u00ea n\u00e3o pode registrar um bot.");
        }

        if (target.getIdLong() == issuer.getIdLong()) {
            return replyTemporary(ctx, "Voc\u00ea n\u00e3o pode registrar voc\u00ea mesmo.");
        }

        if (RegistrationRole.REGISTERED.isPresent(target)) {
            return replyTemporary(ctx, "Este membro j\u00e1 est\u00e1 registrado.");
        }

        if (RegistrationRole.VERIFYING.isPresent(target)) {
            return replyTemporary(ctx, "O usu\u00e1rio ainda est\u00e1 em verifica\u00e7\u00e3o.");
        }

        Guild guild = ctx.getGuild();
        RegistrationRole missingRole = findMissingRole(guild, register.rolesToAdd());

        if (missingRole != null) {
            LOGGER.warn("Could not resolve registration role {}", missingRole);
            return replyTemporary(ctx, "Cargo necess\u00e1rio n\u00e3o encontrado: `" + missingRole.name() + "`.");
        }

        List<Role> rolesAdd = resolveRoles(guild, register.rolesToAdd());
        List<Role> rolesRemove = resolveRoles(guild, register.rolesToRemove());

        ctx.ack(false);
        guild.modifyMemberRoles(target, rolesAdd, rolesRemove).queue((s) -> {
            try {
                RegisterData newRegister = new RegisterData(gender, device, age, target.getIdLong(), issuer.getIdLong());
                logToChannel(guild, target, issuer, rolesAdd);
                registerRepository.save(newRegister);
                deleteLastMessageByUser(ctx.getChannel(), target.getIdLong());
                replyTemporary(ctx, target.getAsMention() + " registrado com sucesso!");
                LOGGER.info("@{} has successfully registered @{}", issuer.getUser().getName(), target.getUser().getName());
            } catch (DataAccessException e) {
                LOGGER.error("Could not insert row to the database", e);
                replyTemporary(ctx, "N\u00e3o foi poss\u00edvel salvar o registro no banco de dados.");
            }
        }, (err) -> {
            replyTemporary(ctx, "Erro :/");
            LOGGER.error("Could not add roles to target {}", target.getId(), err);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Registra um membro no servidor.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a registrar.", true),
                new OptionData(OptionType.STRING, "gender", "O g\u00eanero do membro.", true)
                        .addChoice(RegistrationGender.FEMALE.label(), RegistrationGender.FEMALE.name())
                        .addChoice(RegistrationGender.MALE.label(), RegistrationGender.MALE.name())
                        .addChoice(RegistrationGender.NON_BINARY.label(), RegistrationGender.NON_BINARY.name()),
                new OptionData(OptionType.INTEGER, "age", "A idade do membro.", true)
                        .setRequiredRange(1, 120),
                new OptionData(OptionType.STRING, "device", "O dispositivo principal do membro.", true)
                        .addChoice(RegistrationDevice.MOBILE.label(), RegistrationDevice.MOBILE.name())
                        .addChoice(RegistrationDevice.DESKTOP.label(), RegistrationDevice.DESKTOP.name())
        );
    }

    private boolean checkPermission(Member member) {
        return RegistrationRole.REGISTRAR.isPresent(member) || member.hasPermission(Permission.MANAGE_ROLES);
    }

    private RegistrationRole findMissingRole(Guild guild, List<RegistrationRole> roles) {
        return roles.stream()
                .filter(role -> role.role(guild) == null)
                .findFirst()
                .orElse(null);
    }

    private List<Role> resolveRoles(Guild guild, List<RegistrationRole> roles) {
        return roles.stream()
                .map(role -> role.role(guild))
                .filter(Objects::nonNull)
                .toList();
    }

    private InteractionResult replyTemporary(SlashCommandContext ctx, String content) {
        return ctx.create(false)
                .setContent(content)
                .onSend(hook -> hook.deleteOriginal()
                        .queueAfter(TEMP_RESPONSE_SECONDS, TimeUnit.SECONDS, null, ignoreUnknownMessage()))
                .onHook(message -> message.delete()
                        .queueAfter(TEMP_RESPONSE_SECONDS, TimeUnit.SECONDS, null, ignoreUnknownMessage()))
                .send();
    }

    private ErrorHandler ignoreUnknownMessage() {
        return new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);
    }

    private void deleteLastMessageByUser(MessageChannel channel, long userId) {
        if (!(channel instanceof GuildMessageChannel guildChannel)) return;

        guildChannel.getHistory().retrievePast(20).queue(messages -> {
            for (Message message : messages) {
                if (message.getAuthor().getIdLong() == userId) {
                    message.delete().queue(null, ignoreUnknownMessage());
                }
            }
        });
    }

    private void logToChannel(Guild guild, Member target, Member moderator, List<Role> rolesAdded) {
        Long channelId = Bot.get(REGISTER_LOG_CHANNEL_KEY, Long::parseLong);
        if (channelId == null) {
            LOGGER.warn("Log channel config was not found for key {}", REGISTER_LOG_CHANNEL_KEY);
            return;
        }

        TextChannel channel = guild.getTextChannelById(channelId);

        if (channel == null) {
            LOGGER.warn("Log channel was not found at ID {}", channelId);
            return;
        }

        MessageEmbed embed = getLogEmbed(target, moderator, rolesAdded.stream().map(Role::getAsMention).toList());
        channel.sendMessageEmbeds(embed).queue();
    }

    private MessageEmbed getLogEmbed(Member target, Member moderator, List<String> rolesMention) {
        Guild guild = moderator.getGuild();
        String targetName = target.getUser().getName();
        String moderatorName = moderator.getUser().getName();
        String avatarUrl = target.getUser().getAvatarUrl();
        String roles = String.join("\n", rolesMention);

        return new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("`" + targetName + "` foi registrado!")
                .setThumbnail(avatarUrl)
                .setDescription("Registrado por `" + moderatorName + "`.")
                .addField("Cargos", roles, false)
                .setFooter(guild.getName() + "\u30fbID: " + target.getId(), guild.getIconUrl())
                .build();
    }
}
