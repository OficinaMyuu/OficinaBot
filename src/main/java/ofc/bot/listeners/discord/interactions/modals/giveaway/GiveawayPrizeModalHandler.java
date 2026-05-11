package ofc.bot.listeners.discord.interactions.modals.giveaway;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.domain.entity.enums.GiveawayStatus;
import ofc.bot.domain.sqlite.repository.GiveawayRepository;
import ofc.bot.handlers.giveaway.GiveawayComponentFactory;
import ofc.bot.handlers.giveaway.GiveawayInputParser;
import ofc.bot.handlers.giveaway.GiveawayMessageFactory;
import ofc.bot.handlers.giveaway.GiveawayModalFactory;
import ofc.bot.handlers.giveaway.GiveawayModalFactory.GiveawayCreateDraft;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.modals.contexts.ModalSubmitContext;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@InteractionHandler(scope = Scopes.Giveaway.CREATE, autoResponseType = AutoResponseType.THINKING_EPHEMERAL)
public class GiveawayPrizeModalHandler implements InteractionListener<ModalSubmitContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayPrizeModalHandler.class);
    private final GiveawayRepository giveawayRepo;

    public GiveawayPrizeModalHandler(GiveawayRepository giveawayRepo) {
        this.giveawayRepo = giveawayRepo;
    }

    @Override
    public InteractionResult onExecute(ModalSubmitContext ctx) {
        GiveawayCreateDraft draft = ctx.get("draft");
        Guild guild = ctx.getGuild();
        TextChannel channel = guild.getTextChannelById(draft.channelId());
        if (channel == null) {
            ctx.getSource().getHook().editOriginal("Canal do sorteio não encontrado.").queue();
            return Status.OK;
        }

        PrizeDetails details = parsePrizeDetails(ctx, draft.prizeType());
        if (!details.valid()) {
            ctx.getSource().getHook().editOriginal(details.error()).queue();
            return Status.OK;
        }

        String giveawayId = UUID.randomUUID().toString();
        long now = Bot.unixNow();
        Giveaway giveaway = new Giveaway(
                giveawayId,
                draft.guildId(),
                draft.channelId(),
                0L,
                draft.hostId(),
                GiveawayStatus.ACTIVE,
                draft.prizeType(),
                draft.title(),
                details.description(),
                draft.winnerCount(),
                draft.endsAt(),
                null,
                draft.requiredVoiceChannelId(),
                details.moneyAmount(),
                details.colorRoleDurationSeconds(),
                now,
                now
        );

        channel.sendMessageEmbeds(GiveawayMessageFactory.activeGiveaway(giveaway, 0))
                .setComponents(ActionRow.of(
                        GiveawayComponentFactory.joinButton(giveawayId),
                        GiveawayComponentFactory.leaveButton(giveawayId)
                ))
                .queue(message -> {
                    giveaway.setMessageId(message.getIdLong());
                    giveawayRepo.save(giveaway);
                    ctx.getSource().getHook()
                            .editOriginal("Sorteio criado em " + channel.getAsMention() + ".")
                            .queue();
                }, error -> {
                    LOGGER.error("Failed to publish giveaway {}", giveawayId, error);
                    ctx.getSource().getHook().editOriginal("Não foi possível publicar o sorteio.").queue();
                });

        return Status.OK;
    }

    private PrizeDetails parsePrizeDetails(ModalSubmitContext ctx, GiveawayPrizeType prizeType) {
        String description = normalize(ctx.findField(GiveawayModalFactory.FIELD_DESCRIPTION));

        return switch (prizeType) {
            case GENERIC -> {
                if (description == null) {
                    yield PrizeDetails.invalid("Descrição obrigatória.");
                }
                yield new PrizeDetails(description, null, null, null);
            }
            case ECONOMY_MONEY -> {
                long amount = GiveawayInputParser.parsePositiveMoney(ctx.getField(GiveawayModalFactory.FIELD_AMOUNT));
                if (amount <= 0) {
                    yield PrizeDetails.invalid("Valor inválido. Use um número positivo até " + Bot.fmtMoney(Integer.MAX_VALUE) + ".");
                }
                yield new PrizeDetails(description, amount, null, null);
            }
            case COLOR_ROLE -> {
                long duration = GiveawayInputParser.parseDurationSeconds(ctx.getField(GiveawayModalFactory.FIELD_DURATION));
                if (duration <= 0) {
                    yield PrizeDetails.invalid("Duração inválida. Exemplos: `7d`, `1mo`, `12h`.");
                }
                yield new PrizeDetails(description, null, duration, null);
            }
        };
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private record PrizeDetails(
            String description,
            Long moneyAmount,
            Long colorRoleDurationSeconds,
            String error
    ) {
        static PrizeDetails invalid(String error) {
            return new PrizeDetails(null, null, null, error);
        }

        boolean valid() {
            return error == null;
        }
    }
}
