package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.tables.AccumulatorPrizesTable;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.paginations.Paginator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccumulatorMessageFactoryTest {
    @Test
    void shouldRenderMoneyRowsWithPayCurrencyRejectAndApproveAllControls() {
        AccumulatorPrize prize = money(1, 1000, CurrencyType.OFICINA);
        var page = Paginator.of(offset -> List.of(prize), () -> 1, AccumulatorMessageFactory.PAGE_SIZE).start();

        List<net.dv8tion.jda.api.components.MessageTopLevelComponent> components =
                AccumulatorMessageFactory.createList(null, page);

        Container container = (Container) components.getFirst();
        assertEquals(-5618690, container.getAccentColorRaw());
        assertTrue(container.getComponents().getFirst().asTextDisplay().getContent().contains("Pending prizes"));

        Section section = container.getComponents().get(2).asSection();
        assertEquals("Pay", section.getAccessory().asButton().getLabel());
        assertFalse(section.getAccessory().asButton().isDisabled());
        assertTrue(section.getContentComponents().getFirst().asTextDisplay().getContent().contains("$1.000"));

        ActionRow row = container.getComponents().get(3).asActionRow();
        assertEquals(3, row.getButtons().size());
        assertEquals(ButtonStyle.PRIMARY, row.getButtons().getFirst().getStyle());
        assertEquals(ButtonStyle.DANGER, row.getButtons().get(2).getStyle());

        ActionRow topLevel = (ActionRow) components.get(1);
        assertEquals("Approve All", topLevel.getButtons().get(2).getLabel());
        assertFalse(topLevel.getButtons().get(2).isDisabled());
    }

    @Test
    void shouldDisablePayUntilPrizeIsConfigured() {
        AccumulatorPrize prize = money(1, 1000, null);
        var page = Paginator.of(offset -> List.of(prize), () -> 1, AccumulatorMessageFactory.PAGE_SIZE).start();

        Container container = (Container) AccumulatorMessageFactory.createList(null, page).getFirst();
        Section section = container.getComponents().get(2).asSection();

        assertTrue(section.getAccessory().asButton().isDisabled());
    }

    private AccumulatorPrize money(int id, int amount, CurrencyType currency) {
        AccumulatorPrize prize = new AccumulatorPrize(
                1L,
                10L,
                20L,
                AccumulatorPrizeType.MONEY,
                amount,
                null,
                100L
        );
        prize.set(AccumulatorPrizesTable.ACCUMULATOR_PRIZES.ID, id);
        prize.setCurrency(currency);
        return prize;
    }
}
