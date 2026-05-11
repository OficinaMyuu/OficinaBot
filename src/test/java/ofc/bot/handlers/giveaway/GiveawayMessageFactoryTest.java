package ofc.bot.handlers.giveaway;

import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.domain.entity.enums.GiveawayStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiveawayMessageFactoryTest {
    @Test
    void activeEmbedOmitsVoiceConditionWhenUnset() {
        MessageEmbed embed = GiveawayMessageFactory.activeGiveaway(giveaway(null), 3);

        List<String> fieldNames = embed.getFields().stream()
                .map(MessageEmbed.Field::getName)
                .toList();

        assertFalse(fieldNames.contains("🔊 Requer Participação"));
    }

    @Test
    void activeEmbedShowsVoiceConditionWhenSet() {
        MessageEmbed embed = GiveawayMessageFactory.activeGiveaway(giveaway(99L), 3);

        MessageEmbed.Field voiceField = embed.getFields().stream()
                .filter(field -> "🔊 Requer Participação".equals(field.getName()))
                .findFirst()
                .orElseThrow();

        assertEquals("<#99>", voiceField.getValue());
    }

    @Test
    void claimPromptUsesSorteioCopy() {
        MessageEmbed embed = GiveawayMessageFactory.claimPrompt(giveaway(null));

        assertEquals("Resgate do Sorteio", embed.getTitle());
        assertTrue(embed.getFields().stream().anyMatch(field -> "🎁 Prêmio".equals(field.getName())));
    }

    private Giveaway giveaway(Long requiredVoiceChannelId) {
        return new Giveaway(
                "g-1",
                1L,
                2L,
                3L,
                4L,
                GiveawayStatus.ACTIVE,
                GiveawayPrizeType.GENERIC,
                "Sorteio",
                "Prêmio",
                1,
                200L,
                null,
                requiredVoiceChannelId,
                null,
                null,
                100L,
                100L
        );
    }
}
