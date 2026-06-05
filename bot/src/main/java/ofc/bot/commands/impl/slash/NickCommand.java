package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.handlers.nick.NicknameEmojiPolicy;
import ofc.bot.handlers.nick.NicknameRequestDispatcher;
import ofc.bot.handlers.nick.NicknameTargetPolicy;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "nick", permissions = Permission.NICKNAME_MANAGE)
public class NickCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(NickCommand.class);

    private final NicknameEmojiPolicy emojiPolicy;
    private final NicknameRequestDispatcher dispatcher;
    private final NicknameTargetPolicy targetPolicy;

    public NickCommand(NicknameEmojiPolicy emojiPolicy, NicknameRequestDispatcher dispatcher) {
        this(emojiPolicy, dispatcher, new NicknameTargetPolicy());
    }

    NickCommand(NicknameEmojiPolicy emojiPolicy, NicknameRequestDispatcher dispatcher, NicknameTargetPolicy targetPolicy) {
        this.emojiPolicy = emojiPolicy;
        this.dispatcher = dispatcher;
        this.targetPolicy = targetPolicy;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        String nickname = ctx.getSafeOption("nickname", OptionMapping::getAsString).strip();
        Guild guild = ctx.getGuild();

        if (target == null) {
            MessageEmbed embed = EmbedFactory.embedNicknameDecisionUnavailable("Não consegui encontrar o membro informado.");
            return ctx.create(true)
                    .setEmbeds(embed)
                    .send();
        }

        if (nickname.isBlank()) {
            MessageEmbed embed = EmbedFactory.embedNicknameDecisionUnavailable("O apelido precisa ter pelo menos 1 caractere visível.");
            return ctx.create(true)
                    .setEmbeds(embed)
                    .send();
        }

        if (!guild.getSelfMember().hasPermission(Permission.NICKNAME_MANAGE)) {
            MessageEmbed embed = EmbedFactory.embedNicknameDecisionUnavailable("Eu preciso da permissão `Gerenciar Apelidos` para executar essa alteração.");
            return ctx.create(true)
                    .setEmbeds(embed)
                    .send();
        }

        if (!guild.getSelfMember().canInteract(target)) {
            MessageEmbed embed = EmbedFactory.embedNicknameDecisionUnavailable("Eu não posso alterar o apelido desse membro pela hierarquia atual de cargos.");
            return ctx.create(true)
                    .setEmbeds(embed)
                    .send();
        }

        NicknameTargetPolicy.TargetValidation targetValidation = targetPolicy.validate(ctx.getIssuer(), target);
        if (!targetValidation.accepted()) {
            MessageEmbed embed = EmbedFactory.embedNicknameDecisionUnavailable(targetValidation.rejectionReason());
            return ctx.create(true)
                    .setEmbeds(embed)
                    .send();
        }

        NicknameEmojiPolicy.NicknameEmojiReport report = emojiPolicy.inspect(target, nickname);
        if (report.hasTooManyEmojis()) {
            return ctx.create(true)
                    .setEmbeds(EmbedFactory.embedNicknameValidationRejected(target, nickname, report))
                    .send();
        }

        if (report.hasUnauthorizedStaffEmojis()) {
            return ctx.create(true)
                    .setEmbeds(EmbedFactory.embedNicknameValidationRejected(target, nickname, report))
                    .send();
        }

        submitRequest(ctx, target, nickname, report, false);
        return Status.OK;
    }

    private void submitRequest(
            SlashCommandContext ctx,
            Member target,
            String nickname,
            NicknameEmojiPolicy.NicknameEmojiReport report,
            boolean forced
    ) {
        ctx.ack(true);
        dispatcher.submit(
                ctx.getGuild(),
                target,
                ctx.getUser(),
                nickname,
                report,
                forced,
                request -> ctx.editEmbeds(EmbedFactory.embedNicknameRequestQueued(target, nickname)),
                error -> {
                    LOGGER.error("Failed to queue nickname request for {}", target.getIdLong(), error);
                    ctx.editEmbeds(EmbedFactory.embedNicknameDecisionUnavailable(
                            "Não foi possível enviar o pedido para a fila de aprovação."
                    ));
                }
        );
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Envia um pedido de alteração de apelido para aprovação.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro que terá o apelido alterado.", true),
                new OptionData(OptionType.STRING, "nickname", "O apelido solicitado.", true)
                        .setRequiredLength(1, 32)
        );
    }
}
