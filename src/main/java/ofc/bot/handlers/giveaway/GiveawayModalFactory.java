package ofc.bot.handlers.giveaway;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.handlers.interactions.InteractionMemoryManager;
import ofc.bot.handlers.interactions.modals.contexts.ModalContext;
import ofc.bot.util.Scopes;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GiveawayModalFactory {
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_DURATION = "duration";

    private GiveawayModalFactory() {}

    public static Modal createPrizeModal(GiveawayCreateDraft draft) {
        List<Label> labels = switch (draft.prizeType()) {
            case GENERIC -> genericFields();
            case ECONOMY_MONEY -> economyFields();
            case COLOR_ROLE -> colorRoleFields();
        };

        ModalContext context = ModalContext.of("Detalhes do prêmio", labels)
                .setScope(Scopes.Giveaway.CREATE)
                .setValidity(10, TimeUnit.MINUTES)
                .put("draft", draft);

        InteractionMemoryManager.getManager().save(context);
        return context.getEntity();
    }

    private static List<Label> genericFields() {
        return List.of(
                Label.of(
                        "Descrição",
                        "Explique o que será entregue ao vencedor.",
                        TextInput.create(FIELD_DESCRIPTION, TextInputStyle.PARAGRAPH)
                                .setRequiredRange(1, 1000)
                                .build()
                )
        );
    }

    private static List<Label> economyFields() {
        return List.of(
                Label.of(
                        "Valor",
                        "Quantidade depositada no banco do vencedor.",
                        TextInput.create(FIELD_AMOUNT, TextInputStyle.SHORT)
                                .setRequiredRange(1, 20)
                                .build()
                ),
                Label.of(
                        "Descrição",
                        "Texto opcional mostrado no sorteio.",
                        TextInput.create(FIELD_DESCRIPTION, TextInputStyle.PARAGRAPH)
                                .setMaxLength(1000)
                                .setRequired(false)
                                .build()
                )
        );
    }

    private static List<Label> colorRoleFields() {
        return List.of(
                Label.of(
                        "Duração",
                        "Exemplo: 7d, 1mo, 12h.",
                        TextInput.create(FIELD_DURATION, TextInputStyle.SHORT)
                                .setRequiredRange(1, 40)
                                .build()
                ),
                Label.of(
                        "Descrição",
                        "Texto opcional mostrado no sorteio.",
                        TextInput.create(FIELD_DESCRIPTION, TextInputStyle.PARAGRAPH)
                                .setMaxLength(1000)
                                .setRequired(false)
                                .build()
                )
        );
    }

    public record GiveawayCreateDraft(
            long guildId,
            long hostId,
            long channelId,
            GiveawayPrizeType prizeType,
            String title,
            int winnerCount,
            long endsAt,
            Long requiredVoiceChannelId
    ) {}
}
