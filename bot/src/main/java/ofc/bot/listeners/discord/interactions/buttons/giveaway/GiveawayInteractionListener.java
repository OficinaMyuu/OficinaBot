package ofc.bot.listeners.discord.interactions.buttons.giveaway;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.GiveawayWinner;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.domain.entity.enums.GiveawayWinnerStatus;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.giveaway.GiveawayClaimResult;
import ofc.bot.handlers.giveaway.GiveawayComponentFactory;
import ofc.bot.handlers.giveaway.GiveawayEntryResult;
import ofc.bot.handlers.giveaway.GiveawayMessageFactory;
import ofc.bot.handlers.giveaway.GiveawayService;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@DiscordEventHandler
public class GiveawayInteractionListener extends ListenerAdapter {
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private final GiveawayService giveawayService;

    public GiveawayInteractionListener(GiveawayService giveawayService) {
        this.giveawayService = giveawayService;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        GiveawayComponentFactory.ParsedId id = GiveawayComponentFactory.parse(event.getComponentId());
        if (id == null) {
            return;
        }

        switch (id.action()) {
            case GiveawayComponentFactory.JOIN_ACTION -> handleJoin(event, id.giveawayId());
            case GiveawayComponentFactory.LEAVE_ACTION -> handleLeave(event, id.giveawayId());
            case GiveawayComponentFactory.CLAIM_ACTION -> handleClaim(event, id.giveawayId());
            case GiveawayComponentFactory.CURRENCY_ACTION -> handleCurrency(event, id.giveawayId(), id.extra());
            default -> {
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        GiveawayComponentFactory.ParsedId id = GiveawayComponentFactory.parse(event.getComponentId());
        if (id == null || !GiveawayComponentFactory.COLOR_ACTION.equals(id.action())) {
            return;
        }

        if (event.getMember() == null || event.getValues().isEmpty()) {
            event.reply("Cargo não encontrado.").setEphemeral(true).queue();
            return;
        }

        long roleId;
        try {
            roleId = Long.parseLong(event.getValues().getFirst());
        } catch (NumberFormatException e) {
            event.reply("Cargo não encontrado.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            GiveawayClaimResult result = giveawayService.claimColorRole(id.giveawayId(), event.getMember(), roleId);
            event.getHook()
                    .editOriginalEmbeds(toClaimEmbed(result, "Cargo de cor entregue."))
                    .setComponents(List.of())
                    .queue();
        });
    }

    private void handleJoin(ButtonInteractionEvent event, String giveawayId) {
        if (event.getMember() == null) {
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            GiveawayEntryResult result = giveawayService.enter(giveawayId, event.getMember());
            event.getHook().editOriginal(toEntryMessage(result)).queue();
        });
    }

    private void handleLeave(ButtonInteractionEvent event, String giveawayId) {
        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            GiveawayEntryResult result = giveawayService.leave(giveawayId, event.getUser().getIdLong());
            event.getHook().editOriginal(toEntryMessage(result)).queue();
        });
    }

    private void handleClaim(ButtonInteractionEvent event, String giveawayId) {
        long userId = event.getUser().getIdLong();
        Giveaway giveaway = giveawayService.findGiveaway(giveawayId);
        GiveawayWinner winner = giveawayService.findActiveWinner(giveawayId, userId);

        if (giveaway == null) {
            event.replyEmbeds(GiveawayMessageFactory.claimFailure("Sorteio não encontrado."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (winner == null) {
            event.replyEmbeds(GiveawayMessageFactory.claimFailure("Você não é um vencedor deste sorteio."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (winner.getStatus() != GiveawayWinnerStatus.PENDING_CLAIM) {
            event.replyEmbeds(GiveawayMessageFactory.claimFailure("Esse prêmio já foi processado."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (giveaway.getPrizeType() == GiveawayPrizeType.ECONOMY_MONEY) {
            event.replyEmbeds(GiveawayMessageFactory.claimPrompt(giveaway))
                    .setEphemeral(true)
                    .setComponents(ActionRow.of(
                            GiveawayComponentFactory.currencyButton(giveawayId, CurrencyType.OFICINA),
                            GiveawayComponentFactory.currencyButton(giveawayId, CurrencyType.UNBELIEVABOAT)
                    ))
                    .queue();
            return;
        }

        if (giveaway.getPrizeType() == GiveawayPrizeType.COLOR_ROLE) {
            if (event.getGuild() == null) {
                event.replyEmbeds(GiveawayMessageFactory.claimFailure("Servidor não encontrado."))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            List<ColorRoleItem> colorRoles = giveawayService.findAvailableColorRoles(event.getGuild());
            if (colorRoles.isEmpty()) {
                event.replyEmbeds(GiveawayMessageFactory.claimFailure("Nenhum cargo de cor configurado está disponível."))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            event.replyEmbeds(GiveawayMessageFactory.claimPrompt(giveaway))
                    .setEphemeral(true)
                    .setComponents(ActionRow.of(GiveawayComponentFactory.colorRoleMenu(giveawayId, event.getGuild(), colorRoles)))
                    .queue();
            return;
        }

        event.replyEmbeds(GiveawayMessageFactory.claimPrompt(giveaway)).setEphemeral(true).queue();
    }

    private void handleCurrency(ButtonInteractionEvent event, String giveawayId, String currencyName) {
        CurrencyType currency = CurrencyType.fromName(currencyName);
        if (currency == null || event.getMember() == null) {
            event.reply("Moeda inválida.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            GiveawayClaimResult result = giveawayService.claimMoney(giveawayId, event.getMember(), currency);
            event.getHook()
                    .editOriginalEmbeds(toClaimEmbed(result, "Dinheiro depositado no seu banco."))
                    .setComponents(List.of())
                    .queue();
        });
    }

    private String toEntryMessage(GiveawayEntryResult result) {
        return switch (result) {
            case ENTERED -> "Você entrou no sorteio.";
            case LEFT -> "Você saiu do sorteio.";
            case ALREADY_ENTERED -> "Você já está participando deste sorteio.";
            case NOT_ENTERED -> "Você não está participando deste sorteio.";
            case GIVEAWAY_NOT_FOUND -> "Sorteio não encontrado.";
            case GIVEAWAY_ENDED -> "Esse sorteio já foi encerrado.";
            case BOT_NOT_ALLOWED -> "Bots não podem participar deste sorteio.";
            case MUST_BE_IN_VOICE_CHANNEL -> "Você precisa estar no canal de voz exigido para participar.";
        };
    }

    private net.dv8tion.jda.api.entities.MessageEmbed toClaimEmbed(GiveawayClaimResult result, String successMessage) {
        return switch (result) {
            case CLAIMED -> GiveawayMessageFactory.claimSuccess(successMessage);
            case GIVEAWAY_NOT_FOUND -> GiveawayMessageFactory.claimFailure("Sorteio não encontrado.");
            case NOT_A_WINNER -> GiveawayMessageFactory.claimFailure("Você não é um vencedor deste sorteio.");
            case NOT_CLAIMABLE -> GiveawayMessageFactory.claimFailure("Esse prêmio já foi processado.");
            case WRONG_PRIZE_TYPE -> GiveawayMessageFactory.claimFailure("Esse tipo de prêmio não usa este resgate.");
            case INVALID_COLOR_ROLE -> GiveawayMessageFactory.claimFailure("Esse cargo não é uma cor cadastrada.");
            case ROLE_NOT_FOUND -> GiveawayMessageFactory.claimFailure("Cargo não encontrado.");
            case MEMBER_NOT_FOUND -> GiveawayMessageFactory.claimFailure("Membro não encontrado.");
            case ALREADY_CLAIMING -> GiveawayMessageFactory.claimFailure("Esse prêmio já está sendo processado.");
            case DISCORD_FAILURE -> GiveawayMessageFactory.claimFailure("Não consegui aplicar o cargo.");
            case ECONOMY_FAILURE -> GiveawayMessageFactory.claimFailure("Não consegui depositar o dinheiro.");
        };
    }
}
