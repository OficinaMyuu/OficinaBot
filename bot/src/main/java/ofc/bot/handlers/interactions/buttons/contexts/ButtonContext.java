package ofc.bot.handlers.interactions.buttons.contexts;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import ofc.bot.handlers.interactions.EntityContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ButtonContext extends EntityContext<Button, ButtonContext> {

    public ButtonContext(@NotNull Button entity) {
        super(entity);
    }

    @Override
    public ButtonContext setDisabled(boolean disabled) {
        Button newButton = getEntity().withDisabled(disabled);
        return setEntity(newButton);
    }

    @Override
    public String getId() {
        return getEntity().getCustomId();
    }

    public static ButtonContext of(ButtonStyle style, String label, Emoji emoji) {
        Button btn = Button.of(style, UUID.randomUUID().toString(), label, emoji);
        return new ButtonContext(btn);
    }

    public static ButtonContext of(ButtonStyle style, String label) {
        return of(style, label, null);
    }

    public static ButtonContext of(ButtonStyle style, Emoji emoji) {
        return of(style, null, emoji);
    }

    public static ButtonContext primary(String label, Emoji emoji) {
        return of(ButtonStyle.PRIMARY, label, emoji);
    }

    public static ButtonContext primary(String label) {
        return primary(label, null);
    }

    public static ButtonContext primary(Emoji emoji) {
        return primary(null, emoji);
    }

    public static ButtonContext secondary(String label, Emoji emoji) {
        return of(ButtonStyle.SECONDARY, label, emoji);
    }

    public static ButtonContext secondary(String label) {
        return secondary(label, null);
    }

    public static ButtonContext secondary(Emoji emoji) {
        return secondary(null, emoji);
    }

    public static ButtonContext success(String label, Emoji emoji) {
        return of(ButtonStyle.SUCCESS, label, emoji);
    }

    public static ButtonContext success(String label) {
        return success(label, null);
    }

    public static ButtonContext success(Emoji emoji) {
        return success(null, emoji);
    }

    public static ButtonContext danger(String label, Emoji emoji) {
        return of(ButtonStyle.DANGER, label, emoji);
    }

    public static ButtonContext danger(String label) {
        return danger(label, null);
    }

    public static ButtonContext danger(Emoji emoji) {
        return danger(null, emoji);
    }
}