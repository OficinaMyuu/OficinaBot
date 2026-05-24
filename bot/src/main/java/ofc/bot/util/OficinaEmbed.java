package ofc.bot.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.temporal.TemporalAccessor;
import java.util.function.Supplier;

/**
 * A child class of {@link EmbedBuilder} with some utility methods.
 */
public class OficinaEmbed extends EmbedBuilder {

    @NotNull
    @Override
    public OficinaEmbed clear() {
        super.clear();
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setTitle(@Nullable String title) {
        super.setTitle(title);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setTitle(@Nullable String title, @Nullable String url) {
        super.setTitle(title, url);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setUrl(@Nullable String url) {
        super.setUrl(url);
        return this;
    }

    @NotNull
    public OficinaEmbed setDesc(@Nullable String desc) {
        super.setDescription(desc);
        return this;
    }

    @NotNull
    public OficinaEmbed setDescf(@NotNull String format, Object... args) {
        return setDesc(String.format(format, args));
    }

    public OficinaEmbed setDescIf(boolean expression, @Nullable String desc) {
        if (expression) {
            setDesc(desc);
        }
        return this;
    }

    public OficinaEmbed setDescfIf(boolean expression, @NotNull String format, Object... args) {
        return setDescIf(expression, String.format(format, args));
    }

    @NotNull
    @Override
    public OficinaEmbed appendDescription(@NotNull CharSequence description) {
        super.appendDescription(description);
        return this;
    }

    public OficinaEmbed appendDescf(@NotNull String format, Object... args) {
        return appendDescription(String.format(format, args));
    }

    @NotNull
    public OficinaEmbed appendDescriptionIf(boolean expression, @NotNull CharSequence description) {
        if (expression) {
            appendDescription(description);
        }
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setTimestamp(@Nullable TemporalAccessor temporal) {
        super.setTimestamp(temporal);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setColor(@Nullable Color color) {
        super.setColor(color);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setColor(int color) {
        super.setColor(color);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setThumbnail(@Nullable String url) {
        super.setThumbnail(url);
        return this;
    }

    @NotNull
    public OficinaEmbed setThumbnailIf(boolean expression, Supplier<String> url) {
        if (expression) {
            setThumbnail(url.get());
        }
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setImage(@Nullable String url) {
        super.setImage(url);
        return this;
    }

    @NotNull
    public OficinaEmbed setImageIf(boolean expression, @Nullable String url) {
        if (expression) {
            setImage(url);
        }
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setAuthor(@Nullable String name) {
        super.setAuthor(name);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setAuthor(@Nullable String name, @Nullable String url) {
        super.setAuthor(name, url);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setAuthor(@Nullable String name, @Nullable String url, @Nullable String iconUrl) {
        super.setAuthor(name, url, iconUrl);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setFooter(@Nullable String text) {
        super.setFooter(text);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed setFooter(@Nullable String text, @Nullable String iconUrl) {
        super.setFooter(text, iconUrl);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed addField(@Nullable MessageEmbed.Field field) {
        super.addField(field);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed addField(@NotNull String name, @NotNull String value, boolean inline) {
        super.addField(name, value, inline);
        return this;
    }

    @NotNull
    public OficinaEmbed addField(@NotNull String name, @NotNull String value) {
        return addField(name, value, true);
    }

    @NotNull
    public OficinaEmbed addFieldIf(boolean expression, @NotNull String name, @NotNull String value, boolean inline) {
        if (expression) {
            addField(name, value, inline);
        }
        return this;
    }

    @NotNull
    public OficinaEmbed addFieldIf(boolean expression, @NotNull String name, @NotNull String value) {
        if (expression) {
            addField(name, value, true);
        }
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed addBlankField(boolean inline) {
        super.addBlankField(inline);
        return this;
    }

    @NotNull
    @Override
    public OficinaEmbed clearFields() {
        super.clearFields();
        return this;
    }
}