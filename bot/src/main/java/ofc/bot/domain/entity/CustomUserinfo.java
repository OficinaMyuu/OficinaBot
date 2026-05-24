package ofc.bot.domain.entity;

import ofc.bot.domain.tables.CustomUserinfoTable;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class CustomUserinfo extends OficinaRecord<CustomUserinfo> {
    private static final CustomUserinfoTable CUSTOM_USERINFO = CustomUserinfoTable.CUSTOM_USERINFO;

    public static final String MARRIAGE_FORMAT = "💕 %s (<t:%d:d>)\n";
    public static final String DEFAULT_TITLE_FORMAT = "👥 %s";
    public static final String DEFAULT_DESCRIPTION_FORMAT = "Informações de `%s` <a:M_Myuu:643942157325041668>";

    public CustomUserinfo() {
        super(CUSTOM_USERINFO);
    }

    public CustomUserinfo(long userId, int color, String description, String footer, long createdAt, long updatedAt) {
        this();
        set(CUSTOM_USERINFO.USER_ID, userId);
        set(CUSTOM_USERINFO.COLOR, color);
        set(CUSTOM_USERINFO.DESCRIPTION, description);
        set(CUSTOM_USERINFO.FOOTER, footer);
        set(CUSTOM_USERINFO.CREATED_AT, createdAt);
        set(CUSTOM_USERINFO.UPDATED_AT, updatedAt);
    }

    public static CustomUserinfo fromUserId(long userId) {
        long now = Bot.unixNow();
        return new CustomUserinfo(userId, 0, null, null, now, now);
    }

    public CustomUserinfo(long userId, long createdAt, long updatedAt) {
        this(userId, 0, null, null, createdAt, updatedAt);
    }

    public long getUserId() {
        return get(CUSTOM_USERINFO.USER_ID);
    }

    public int getColorRaw() {
        Integer color = get(CUSTOM_USERINFO.COLOR);
        return color == null ? 0 : color;
    }

    public Color getColor() {
        return getColorRaw() == 0 ? null : new Color(getColorRaw());
    }

    public String getDescription() {
        return get(CUSTOM_USERINFO.DESCRIPTION);
    }

    public String getFooter() {
        return get(CUSTOM_USERINFO.FOOTER);
    }

    public long getTimeCreated() {
        return get(CUSTOM_USERINFO.CREATED_AT);
    }

    public long getLastUpdated() {
        return get(CUSTOM_USERINFO.UPDATED_AT);
    }

    public CustomUserinfo setUserId(long userId) {
        set(CUSTOM_USERINFO.USER_ID, userId);
        return this;
    }

    public CustomUserinfo setColor(int color) {
        set(CUSTOM_USERINFO.COLOR, color);
        return this;
    }

    public CustomUserinfo setDescription(String desc) {
        set(CUSTOM_USERINFO.DESCRIPTION, desc);
        return this;
    }

    public CustomUserinfo setFooter(String footer) {
        set(CUSTOM_USERINFO.FOOTER, footer);
        return this;
    }

    @NotNull
    public CustomUserinfo setLastUpdated(long updateAt) {
        set(CUSTOM_USERINFO.UPDATED_AT, updateAt);
        return this;
    }
}