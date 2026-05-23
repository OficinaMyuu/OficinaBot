package ofc.bot.handlers.shop;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import ofc.bot.domain.entity.ColorRoleState;

import java.util.ArrayList;
import java.util.List;

public final class ColorRoleStoreMessageFactory {
    private static final int ACCENT_COLOR = 240116;

    private ColorRoleStoreMessageFactory() {}

    public static Container create(List<Entry> entries) {
        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(TextDisplay.of("""
                ### Colors
                Shows all available colors, their price and expiry date.
                """.strip()));
        components.add(divider());

        for (Entry entry : entries) {
            components.add(toSection(entry));
            components.add(divider());
        }

        components.add(TextDisplay.of("Page 1/1"));
        return Container.of(components).withAccentColor(ACCENT_COLOR);
    }

    private static Section toSection(Entry entry) {
        return Section.of(entry.button(), TextDisplay.of(formatEntry(entry)));
    }

    private static String formatEntry(Entry entry) {
        ColorRoleState state = entry.state();
        if (state == null) {
            return String.format("**<@&%d>**", entry.roleId());
        }

        return String.format("**<@&%d>**\n-# Expires at <t:%d>.", entry.roleId(), state.getExpiresAt());
    }

    private static Separator divider() {
        return Separator.createDivider(Separator.Spacing.SMALL);
    }

    public record Entry(long roleId, ColorRoleState state, Button button) {}
}
