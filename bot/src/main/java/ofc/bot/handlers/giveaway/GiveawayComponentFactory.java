package ofc.bot.handlers.giveaway;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.Giveaway;
import ofc.bot.domain.entity.enums.GiveawayPrizeType;
import ofc.bot.handlers.economy.CurrencyType;

import java.util.List;
import java.util.Objects;

public final class GiveawayComponentFactory {
    public static final String PREFIX = "giveaway:";
    public static final String JOIN_ACTION = "join";
    public static final String LEAVE_ACTION = "leave";
    public static final String CLAIM_ACTION = "claim";
    public static final String CURRENCY_ACTION = "currency";
    public static final String COLOR_ACTION = "color";

    private GiveawayComponentFactory() {}

    public static Button joinButton(String giveawayId) {
        return Button.of(ButtonStyle.SUCCESS, customId(JOIN_ACTION, giveawayId), "Participar");
    }

    public static Button leaveButton(String giveawayId) {
        return Button.of(ButtonStyle.SECONDARY, customId(LEAVE_ACTION, giveawayId), "Sair");
    }

    public static Button claimButton(String giveawayId) {
        return Button.of(ButtonStyle.PRIMARY, customId(CLAIM_ACTION, giveawayId), "Resgatar prêmio");
    }

    public static Button currencyButton(String giveawayId, CurrencyType currency) {
        return Button.of(
                ButtonStyle.SUCCESS,
                customId(CURRENCY_ACTION, giveawayId, currency.name()),
                currency.getName(),
                currency.getEmoji()
        );
    }

    public static StringSelectMenu colorRoleMenu(String giveawayId, Guild guild, List<ColorRoleItem> colorRoles) {
        List<SelectOption> options = colorRoles.stream()
                .map(item -> guild.getRoleById(item.getRoleId()))
                .filter(Objects::nonNull)
                .map(role -> SelectOption.of(label(role), role.getId()))
                .limit(SelectMenu.OPTIONS_MAX_AMOUNT)
                .toList();

        return StringSelectMenu.create(customId(COLOR_ACTION, giveawayId))
                .setPlaceholder("Escolha um cargo de cor")
                .addOptions(options)
                .setRequiredRange(1, 1)
                .build();
    }

    private static String label(Role role) {
        return role.getName().length() > 100 ? role.getName().substring(0, 100) : role.getName();
    }

    public static boolean requiresClaim(Giveaway giveaway) {
        return giveaway.getPrizeType() != GiveawayPrizeType.GENERIC;
    }

    public static String customId(String action, String giveawayId, String... parts) {
        StringBuilder builder = new StringBuilder(PREFIX)
                .append(action)
                .append(':')
                .append(giveawayId);

        for (String part : parts) {
            builder.append(':').append(part);
        }
        return builder.toString();
    }

    public static ParsedId parse(String customId) {
        if (customId == null || !customId.startsWith(PREFIX)) {
            return null;
        }

        String[] tokens = customId.split(":");
        if (tokens.length < 3 || !"giveaway".equals(tokens[0])) {
            return null;
        }

        String action = tokens[1];
        String giveawayId = tokens[2];
        String extra = tokens.length >= 4 ? tokens[3] : null;
        return new ParsedId(action, giveawayId, extra);
    }

    public record ParsedId(String action, String giveawayId, String extra) {}
}
