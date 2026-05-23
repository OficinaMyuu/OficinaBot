package ofc.bot.handlers.shop;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import ofc.bot.domain.entity.ColorRoleState;
import ofc.bot.handlers.economy.CurrencyType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorRoleStoreMessageFactoryTest {
    @Test
    void shouldRenderHeaderRowsAndFooterAsComponentsV2Container() {
        Container container = ColorRoleStoreMessageFactory.create(List.of(
                new ColorRoleStoreMessageFactory.Entry(10L, null, buyButton()),
                new ColorRoleStoreMessageFactory.Entry(20L, state(3000L), removeButton())
        ));

        assertEquals(-5618690, container.getAccentColorRaw());
        assertEquals("### Cores\nMostra todas as cores disponíveis, o preço e a data de remoção.",
                container.getComponents().getFirst().asTextDisplay().getContent());
        assertEquals("Pág 1/1", container.getComponents().getLast().asTextDisplay().getContent());

        Section available = container.getComponents().get(2).asSection();
        Section owned = container.getComponents().get(4).asSection();

        assertEquals("**<@&10>**", firstText(available).getContent());
        assertEquals(ButtonStyle.SUCCESS, available.getAccessory().asButton().getStyle());
        assertEquals("500", available.getAccessory().asButton().getLabel());

        assertEquals("**<@&20>**\n-# Expira em <t:3000>.", firstText(owned).getContent());
        assertEquals(ButtonStyle.DANGER, owned.getAccessory().asButton().getStyle());
        assertEquals("250", owned.getAccessory().asButton().getLabel());
    }

    @Test
    void shouldSeparateEveryStoreRowWithDividers() {
        Container container = ColorRoleStoreMessageFactory.create(List.of(
                new ColorRoleStoreMessageFactory.Entry(10L, null, buyButton())
        ));

        assertTrue(container.getComponents().get(1).asSeparator().isDivider());
        assertTrue(container.getComponents().get(3).asSeparator().isDivider());
    }

    private TextDisplay firstText(Section section) {
        return section.getContentComponents().getFirst().asTextDisplay();
    }

    private Button buyButton() {
        return Button.of(ButtonStyle.SUCCESS, "buy", "500", Emoji.fromUnicode("\uD83D\uDECD\uFE0F"));
    }

    private Button removeButton() {
        return Button.of(ButtonStyle.DANGER, "remove", "250", Emoji.fromUnicode("\uD83D\uDDD1"));
    }

    private ColorRoleState state(long expiresAt) {
        return new ColorRoleState(
                250,
                CurrencyType.OFICINA,
                1L,
                2L,
                20L,
                expiresAt,
                1000L,
                1000L
        );
    }
}
