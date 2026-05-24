package ofc.bot.listeners.discord.interactions.buttons.nick;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.NicknameUpdateRequest;
import ofc.bot.domain.entity.enums.NicknameRequestStatus;
import ofc.bot.domain.sqlite.repository.NicknameUpdateRequestRepository;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DiscordEventHandler
public class NicknameApprovalButtonListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NicknameApprovalButtonListener.class);
    private static final String BUTTON_PREFIX = "nick-";
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final NicknameUpdateRequestRepository requestRepo;

    public NicknameApprovalButtonListener(NicknameUpdateRequestRepository requestRepo) {
        this.requestRepo = Objects.requireNonNull(requestRepo);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (!buttonId.startsWith(BUTTON_PREFIX)) {
            return;
        }

        Guild guild = event.getGuild();
        Member actor = event.getMember();
        if (guild == null || actor == null) {
            return;
        }

        NicknameUpdateRequest request = requestRepo.findByButtonId(buttonId);
        if (request == null) {
            event.replyEmbeds(EmbedFactory.embedNicknameDecisionUnavailable("Esse pedido de apelido não foi encontrado."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!actor.hasPermission(Permission.NICKNAME_MANAGE)) {
            event.replyEmbeds(EmbedFactory.embedNicknameDecisionUnavailable("Você precisa da permissão `Gerenciar Apelidos` para decidir esse pedido."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (request.getStatus() != NicknameRequestStatus.PENDING) {
            event.replyEmbeds(EmbedFactory.embedNicknameDecisionUnavailable("Esse pedido já foi processado."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferEdit().queue();
        boolean approve = buttonId.equals(request.getApproveButtonId());
        EXECUTOR.execute(() -> {
            if (approve) {
                approve(event, request, actor.getIdLong());
            } else {
                reject(event, request, actor.getIdLong());
            }
        });
    }

    private void approve(ButtonInteractionEvent event, NicknameUpdateRequest request, long actorId) {
        long now = Bot.nowMillis();
        if (!requestRepo.startProcessing(request.getRequestId(), actorId, now)) {
            return;
        }

        Guild guild = event.getGuild();
        if (guild == null) {
            requestRepo.markFailed(request.getRequestId(), actorId, Bot.nowMillis());
            return;
        }

        Member self = guild.getSelfMember();
        try {
            Member target = guild.retrieveMemberById(request.getTargetUserId()).complete();

            if (!self.canInteract(target)) {
                fail(event, request, actorId, "Eu não posso alterar o apelido desse membro pela hierarquia atual de cargos.");
                return;
            }

            target.modifyNickname(request.getNickname())
                    .reason("Requested by: " + request.getSubmittedById())
                    .complete();

            requestRepo.markApproved(request.getRequestId(), actorId, Bot.nowMillis());
            event.getMessage()
                    .editMessageEmbeds(EmbedFactory.embedNicknameApproved(
                            target,
                            actorId,
                            request.getNickname(),
                            request.getEmojiApprovalSummary(),
                            request.getUnauthorizedSummary()
                    ))
                    .setComponents(List.of())
                    .queue();
        } catch (Exception e) {
            LOGGER.error("Failed to approve nickname request {}", request.getRequestId(), e);
            fail(event, request, actorId, "Não foi possível aplicar esse apelido.");
        }
    }

    private void reject(ButtonInteractionEvent event, NicknameUpdateRequest request, long actorId) {
        long now = Bot.nowMillis();
        if (!requestRepo.markRejected(request.getRequestId(), actorId, now)) {
            return;
        }

        try {
            User target = event.getJDA().retrieveUserById(request.getTargetUserId()).complete();
            event.getMessage()
                    .editMessageEmbeds(EmbedFactory.embedNicknameRejected(
                            target,
                            actorId,
                            request.getNickname(),
                            request.getEmojiApprovalSummary(),
                            request.getUnauthorizedSummary()
                    ))
                    .setComponents(List.of())
                    .queue();
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch target user while rejecting nickname request {}", request.getRequestId(), e);
            event.getMessage()
                    .editMessageEmbeds(EmbedFactory.embedNicknameDecisionUnavailable("Pedido rejeitado, mas não consegui carregar o usuário."))
                    .setComponents(List.of())
                    .queue();
        }
    }

    private void fail(ButtonInteractionEvent event, NicknameUpdateRequest request, long actorId, String reason) {
        requestRepo.markFailed(request.getRequestId(), actorId, Bot.nowMillis());
        event.getMessage()
                .editMessageEmbeds(EmbedFactory.embedNicknameDecisionUnavailable(reason))
                .setComponents(List.of())
                .queue();
    }
}
