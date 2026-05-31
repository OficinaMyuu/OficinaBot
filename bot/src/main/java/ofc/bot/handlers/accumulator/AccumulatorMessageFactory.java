package ofc.bot.handlers.accumulator;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.entity.enums.AccumulatorPrizeType;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.util.Bot;
import ofc.bot.util.OficinaEmbed;
import ofc.bot.util.embeds.EmbedFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class AccumulatorMessageFactory {
    public static final int PAGE_SIZE = 6;
    private static final String PREFIX = "acc";
    private static final String VERSION = "v1";
    private static final Color ACCENT_COLOR = Bot.Colors.DEFAULT;

    private AccumulatorMessageFactory() {}

    public static List<MessageTopLevelComponent> createList(Guild guild, PageItem<AccumulatorPrize> page) {
        List<ContainerChildComponent> children = new ArrayList<>();

        children.add(TextDisplay.of(String.format("""
                ### Accumulator
                Pending prizes: `%s`.
                """.strip(), Bot.fmtNum(page.getRowCount()))));
        children.add(divider());

        if (page.isEmpty()) {
            children.add(TextDisplay.of("*No pending prizes.*"));
        } else {
            for (AccumulatorPrize prize : page.getEntities()) {
                children.add(Section.of(payButton(prize, page.getPageIndex()), TextDisplay.of(formatPrize(guild, prize))));
                children.add(ActionRow.of(rowButtons(prize, page.getPageIndex())));
                children.add(divider());
            }
        }

        children.add(TextDisplay.of(String.format("Pag %s/%s", page.getPage(), page.getPageCount())));
        Container container = Container.of(children).withAccentColor(ACCENT_COLOR);
        ActionRow actions = ActionRow.of(
                Button.secondary(pageId(page.getPageIndex() - 1), Bot.Emojis.GRAY_ARROW_LEFT)
                        .withDisabled(page.getPageIndex() <= 0),
                Button.secondary(pageId(page.getPageIndex() + 1), Bot.Emojis.GRAY_ARROW_RIGHT)
                        .withDisabled(!page.hasMore()),
                Button.success(approveAllId(page.getPageIndex()), "Approve All")
                        .withDisabled(page.getRowCount() <= 0)
        );

        return List.of(container, actions);
    }

    public static StringSelectMenu colorRoleMenu(
            int prizeId,
            int pageIndex,
            long channelId,
            long messageId,
            Guild guild,
            List<ColorRoleItem> colorRoles
    ) {
        StringSelectMenu.Builder builder = StringSelectMenu.create(colorSelectId(prizeId, pageIndex, channelId, messageId))
                .setPlaceholder("Choose a color role")
                .setRequiredRange(1, 1);

        colorRoles.stream()
                .filter(item -> guild.getRoleById(item.getRoleId()) != null)
                .limit(SelectMenu.OPTIONS_MAX_AMOUNT)
                .map(item -> toOption(guild, item))
                .forEach(builder::addOptions);

        return builder.build();
    }

    public static MessageEmbed addSuccess(User user, AccumulatorPrizeType type, int count) {
        return new OficinaEmbed()
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setColor(EmbedFactory.OK_GREEN)
                .setTitle("Accumulator updated")
                .setDescf("Added `%s` %s prize(s) to the pending box.", Bot.fmtNum(count), type.getDisplay())
                .build();
    }

    public static MessageEmbed failure(String title, String description) {
        return new OficinaEmbed()
                .setTitle(title)
                .setColor(EmbedFactory.DANGER_RED)
                .setDesc(description)
                .build();
    }

    public static MessageEmbed setupSuccess(String description) {
        return new OficinaEmbed()
                .setTitle("Accumulator updated")
                .setColor(EmbedFactory.OK_GREEN)
                .setDesc(description)
                .build();
    }

    public static MessageEmbed approvalReport(AccumulatorApprovalReport report, User user) {
        Color color = report.successful() ? EmbedFactory.OK_GREEN : EmbedFactory.DANGER_RED;
        String details = report.details().isEmpty()
                ? "No additional details."
                : Bot.limitStr(String.join("\n", report.details()), MessageEmbed.DESCRIPTION_MAX_LENGTH - 500);

        return new OficinaEmbed()
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setTitle(report.successful() ? "Accumulator payout complete" : "Accumulator payout failed")
                .setColor(color)
                .setDesc(report.summary())
                .addField("Requested", Bot.fmtNum(report.requested()), true)
                .addField("Paid", Bot.fmtNum(report.paid()), true)
                .addField("Elapsed", report.elapsedMillis() + " ms", true)
                .addField("Report", details, false)
                .build();
    }

    public static boolean isAccumulatorId(String customId) {
        return customId != null && customId.startsWith(PREFIX + ":" + VERSION + ":");
    }

    public static String[] parts(String customId) {
        return customId.split(":");
    }

    public static String pageId(int pageIndex) {
        return id("page", Integer.toString(pageIndex));
    }

    public static String approveAllId(int pageIndex) {
        return id("all", Integer.toString(pageIndex));
    }

    public static String payId(int prizeId, int pageIndex) {
        return id("pay", Integer.toString(prizeId), Integer.toString(pageIndex));
    }

    public static String rejectId(int prizeId, int pageIndex) {
        return id("reject", Integer.toString(prizeId), Integer.toString(pageIndex));
    }

    public static String currencyId(int prizeId, int pageIndex, CurrencyType currency) {
        return id("currency", Integer.toString(prizeId), Integer.toString(pageIndex), currency.name());
    }

    public static String colorButtonId(int prizeId, int pageIndex) {
        return id("color", Integer.toString(prizeId), Integer.toString(pageIndex));
    }

    public static String colorSelectId(int prizeId, int pageIndex, long channelId, long messageId) {
        return id(
                "color_select",
                Integer.toString(prizeId),
                Integer.toString(pageIndex),
                Long.toString(channelId),
                Long.toString(messageId)
        );
    }

    private static List<ActionRowChildComponent> rowButtons(AccumulatorPrize prize, int pageIndex) {
        List<ActionRowChildComponent> buttons = new ArrayList<>();

        if (prize.getType() == AccumulatorPrizeType.MONEY) {
            buttons.add(currencyButton(prize, pageIndex, CurrencyType.OFICINA));
            buttons.add(currencyButton(prize, pageIndex, CurrencyType.UNBELIEVABOAT));
        } else {
            String label = prize.getColorRoleId() == null ? "Set Color" : "Change Color";
            buttons.add(Button.secondary(colorButtonId(prize.getId(), pageIndex), label));
        }

        buttons.add(Button.of(ButtonStyle.DANGER, rejectId(prize.getId(), pageIndex), "Reject", Bot.Emojis.TRASH));
        return buttons;
    }

    private static Button payButton(AccumulatorPrize prize, int pageIndex) {
        return Button.success(payId(prize.getId(), pageIndex), "Pay")
                .withDisabled(!isReady(prize));
    }

    private static Button currencyButton(AccumulatorPrize prize, int pageIndex, CurrencyType currency) {
        ButtonStyle style = currency == prize.getCurrency() ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY;
        return Button.of(style, currencyId(prize.getId(), pageIndex, currency), currency.getName(), currency.getEmoji());
    }

    private static boolean isReady(AccumulatorPrize prize) {
        return switch (prize.getType()) {
            case MONEY -> prize.getCurrency() != null;
            case COLOR_ROLE -> prize.getColorRoleId() != null;
        };
    }

    private static String formatPrize(Guild guild, AccumulatorPrize prize) {
        String target = "<@" + prize.getTargetId() + ">";
        String creator = "<@" + prize.getCreatedBy() + ">";
        String lastError = prize.getLastError();
        String errorLine = lastError == null || lastError.isBlank()
                ? ""
                : "\n-# Last error: " + Bot.limitStr(lastError.replace("\n", " "), 120);

        if (prize.getType() == AccumulatorPrizeType.MONEY) {
            String currency = prize.getCurrency() == null ? "not chosen" : prize.getCurrency().getFormatted();
            return String.format(
                    "**#%d - %s**\nTarget: %s\nAmount: `$%s`\nCurrency: %s\n-# Added by %s.%s",
                    prize.getId(),
                    prize.getType().getDisplay(),
                    target,
                    Bot.fmtNum(prize.getAmount()),
                    currency,
                    creator,
                    errorLine
            );
        }

        Long roleId = prize.getColorRoleId();
        Role role = roleId == null ? null : guild.getRoleById(roleId);
        String roleDisplay = role == null
                ? roleId == null ? "not chosen" : "missing role `" + roleId + "`"
                : role.getAsMention();

        return String.format(
                "**#%d - %s**\nTarget: %s\nDuration: `%s`\nColor: %s\n-# Added by %s.%s",
                prize.getId(),
                prize.getType().getDisplay(),
                target,
                Bot.parsePeriod(prize.getColorDurationSeconds()),
                roleDisplay,
                creator,
                errorLine
        );
    }

    private static SelectOption toOption(Guild guild, ColorRoleItem item) {
        Role role = guild.getRoleById(item.getRoleId());
        String label = role == null ? Long.toString(item.getRoleId()) : role.getName();
        return SelectOption.of(Bot.limitStr(label, 100), Long.toString(item.getRoleId()));
    }

    private static Separator divider() {
        return Separator.createDivider(Separator.Spacing.SMALL);
    }

    private static String id(String action, String... values) {
        StringBuilder builder = new StringBuilder(PREFIX)
                .append(':')
                .append(VERSION)
                .append(':')
                .append(action);

        for (String value : values) {
            builder.append(':').append(value);
        }
        return builder.toString();
    }
}
