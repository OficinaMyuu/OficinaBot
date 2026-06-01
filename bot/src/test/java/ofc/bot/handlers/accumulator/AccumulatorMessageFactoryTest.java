package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.domain.tables.AccumulatorPrizesTable;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.paginations.Paginator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
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
        assertTrue(container.getComponents().getFirst().asTextDisplay().getContent().contains("pendentes"));

        Section section = container.getComponents().get(2).asSection();
        assertEquals("Pagar", section.getAccessory().asButton().getLabel());
        assertFalse(section.getAccessory().asButton().isDisabled());
        assertTrue(section.getContentComponents().getFirst().asTextDisplay().getContent().contains("$1.000"));

        ActionRow row = container.getComponents().get(3).asActionRow();
        assertEquals(3, row.getButtons().size());
        assertEquals(ButtonStyle.PRIMARY, row.getButtons().getFirst().getStyle());
        assertTrue(row.getButtons().getFirst().isDisabled());
        assertEquals(ButtonStyle.SECONDARY, row.getButtons().get(1).getStyle());
        assertFalse(row.getButtons().get(1).isDisabled());
        assertEquals(ButtonStyle.DANGER, row.getButtons().get(2).getStyle());

        ActionRow topLevel = (ActionRow) components.get(1);
        assertEquals("Aprovar Tudo", topLevel.getButtons().get(2).getLabel());
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

    @Test
    void shouldRenderMoneyPrizeWithMissingAmountAsZero() {
        AccumulatorPrize prize = money(1, null, CurrencyType.OFICINA);
        var page = Paginator.of(offset -> List.of(prize), () -> 1, AccumulatorMessageFactory.PAGE_SIZE).start();

        Container container = (Container) AccumulatorMessageFactory.createList(null, page).getFirst();
        Section section = container.getComponents().get(2).asSection();

        assertTrue(section.getContentComponents().getFirst().asTextDisplay().getContent().contains("$0"));
    }

    @Test
    void shouldRenderColorPrizeWithMissingDurationAsZero() {
        AccumulatorPrize prize = color(1, null, null);
        var page = Paginator.of(offset -> List.of(prize), () -> 1, AccumulatorMessageFactory.PAGE_SIZE).start();

        Container container = (Container) AccumulatorMessageFactory.createList(null, page).getFirst();
        Section section = container.getComponents().get(2).asSection();

        assertTrue(section.getContentComponents().getFirst().asTextDisplay().getContent().contains("`0s`"));
    }

    @Test
    void shouldPrefixApprovalReportFieldsWithEmojis() {
        AccumulatorApprovalReport report = AccumulatorApprovalReport.success(2, 15, "Pago.", List.of());

        MessageEmbed embed = AccumulatorMessageFactory.approvalReport(report, guild(), user());

        assertEquals("🎯 Solicitados", embed.getFields().get(0).getName());
        assertEquals("✅ Pagos", embed.getFields().get(1).getName());
        assertEquals("⏱️ Tempo", embed.getFields().get(2).getName());
        assertEquals("📄 Relatório", embed.getFields().get(3).getName());
    }

    private AccumulatorPrize money(int id, Integer amount, CurrencyType currency) {
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

    private AccumulatorPrize color(int id, Long roleId, Long duration) {
        AccumulatorPrize prize = new AccumulatorPrize(
                1L,
                10L,
                20L,
                AccumulatorPrizeType.COLOR_ROLE,
                null,
                duration,
                100L
        );
        prize.set(AccumulatorPrizesTable.ACCUMULATOR_PRIZES.ID, id);
        prize.setColorRoleId(roleId);
        return prize;
    }

    private Guild guild() {
        return proxy(Guild.class);
    }

    private User user() {
        return proxy(User.class);
    }

    private <T> T proxy(Class<T> type) {
        Object proxy = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (instance, method, args) -> switch (method.getName()) {
                    case "getName" -> "Oficina";
                    case "getEffectiveAvatarUrl", "getIconUrl" -> null;
                    case "toString" -> type.getSimpleName() + "Proxy";
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
        return type.cast(proxy);
    }
}
