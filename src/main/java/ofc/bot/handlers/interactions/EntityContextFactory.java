package ofc.bot.handlers.interactions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.modals.Modal;
import ofc.bot.domain.entity.*;
import ofc.bot.domain.entity.enums.GroupPermission;
import ofc.bot.domain.entity.enums.NameScope;
import ofc.bot.domain.entity.enums.TransactionType;
import ofc.bot.domain.viewmodels.LeaderboardUser;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.games.betting.tictactoe.GameGrid;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonContext;
import ofc.bot.handlers.interactions.modals.contexts.ModalContext;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Bot;
import ofc.bot.util.Scopes;

import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class EntityContextFactory {
    private static final InteractionMemoryManager INTERACTION_MANAGER = InteractionMemoryManager.getManager();
    private static final String ZERO_WIDTH_SPACE = "\u200E";
    private static final Emoji GAME_EMOJI = Emoji.fromUnicode("🎮");
    private static final Emoji TRASH_EMOJI = Emoji.fromUnicode("🗑");

    private EntityContextFactory() {}

    /* -------------------- Buttons -------------------- */
    public static List<Button> createBirthdayListButtons(Month currMonth) {
        Month previousMonth = currMonth.minus(1);
        Month nextMonth = currMonth.plus(1);
        boolean hasPrevious = currMonth != Month.JANUARY;
        boolean hasNext = currMonth != Month.DECEMBER;

        ButtonContext prevButton = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Misc.PAGINATE_BIRTHDAYS)
                .put("month", previousMonth)
                .setEnabled(hasPrevious);

        ButtonContext nextButton = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Misc.PAGINATE_BIRTHDAYS)
                .put("month", nextMonth)
                .setEnabled(hasNext);

        INTERACTION_MANAGER.save(prevButton, nextButton);
        return List.of(prevButton.getEntity(), nextButton.getEntity());
    }

    public static List<Button> createTransactionsButtons(long userId, List<CurrencyType> currencies,
                                                         List<TransactionType> actions,
                                                         int pageIndex, boolean hasNext) {
        boolean hasPrevious = pageIndex > 0;

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Economy.VIEW_TRANSACTIONS)
                .put("page_index", pageIndex - 1)
                .put("user_id", userId)
                .put("currencies", currencies)
                .put("actions", actions)
                .setEnabled(hasPrevious);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Economy.VIEW_TRANSACTIONS)
                .put("page_index", pageIndex + 1)
                .put("user_id", userId)
                .put("currencies", currencies)
                .put("actions", actions)
                .setEnabled(hasNext);

        INTERACTION_MANAGER.save(prev, next);
        return List.of(prev.getEntity(), next.getEntity());
    }

    public static List<Button> createInfractionsButtons(int infrId, boolean isActive,
                                                        long targetId, int pageIndex,
                                                        boolean showInactive, boolean hasNext) {
        boolean hasPrevious = pageIndex > 0;

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Punishments.VIEW_INFRACTIONS)
                .setPermission(Permission.MESSAGE_MANAGE)
                .put("page_index", pageIndex - 1)
                .put("target_id", targetId)
                .put("show_inactive", showInactive)
                .setEnabled(hasPrevious);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Punishments.VIEW_INFRACTIONS)
                .setPermission(Permission.MESSAGE_MANAGE)
                .put("page_index", pageIndex + 1)
                .put("target_id", targetId)
                .put("show_inactive", showInactive)
                .setEnabled(hasNext);

        ButtonContext delete = ButtonContext.danger(Emoji.fromUnicode("🗑"))
                .setScope(Scopes.Punishments.DELETE_INFRACTION)
                .setPermission(Permission.MANAGE_SERVER)
                .put("page_index", pageIndex)
                .put("infraction_id", infrId)
                .put("target_id", targetId)
                .put("show_inactive", showInactive)
                .setEnabled(isActive);

        INTERACTION_MANAGER.save(prev, next, delete);
        return List.of(prev.getEntity(), next.getEntity(), delete.getEntity());
    }

    public static List<Button> createLeaderboardButtons(Paginator<LeaderboardUser> paginator, int pageIndex, boolean hasNext) {
        boolean hasPrevious = pageIndex > 0;

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Economy.VIEW_LEADERBOARD)
                .put("page_index", pageIndex - 1)
                .put("paginator", paginator)
                .setEnabled(hasPrevious);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Economy.VIEW_LEADERBOARD)
                .put("page_index", pageIndex + 1)
                .put("paginator", paginator)
                .setEnabled(hasNext);

        INTERACTION_MANAGER.save(prev, next);
        return List.of(prev.getEntity(), next.getEntity());
    }

    public static List<Button> createRemindersButtons(Reminder rem, int pageIndex, boolean hasNext) {
        long userId = rem.getUserId();
        boolean hasPrevious = pageIndex > 0;

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Reminders.VIEW_REMINDERS)
                .put("page_index", pageIndex - 1)
                .addUser(userId)
                .setEnabled(hasPrevious);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Reminders.VIEW_REMINDERS)
                .put("page_index", pageIndex + 1)
                .addUser(userId)
                .setEnabled(hasNext);

        ButtonContext delete = ButtonContext.danger(Emoji.fromUnicode("🗑"))
                .setScope(Scopes.Reminders.DELETE_REMINDER)
                .addUser(userId)
                .put("page_index", pageIndex)
                .put("reminder_id", rem.getId())
                .setEnabled(!rem.isExpired());

        INTERACTION_MANAGER.save(prev, next, delete);
        return List.of(prev.getEntity(), next.getEntity(), delete.getEntity());
    }

    public static List<Button> createLevelsButtons(long userId, int pageIndex, boolean hasNext) {
        boolean hasPrevious = pageIndex > 0;

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Misc.PAGINATE_LEVELS)
                .put("user_id", userId)
                .put("page_index", pageIndex - 1)
                .setEnabled(hasPrevious);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Misc.PAGINATE_LEVELS)
                .put("user_id", userId)
                .put("page_index", pageIndex + 1)
                .setEnabled(hasNext);

        INTERACTION_MANAGER.save(prev, next);
        return List.of(prev.getEntity(), next.getEntity());
    }

    public static List<Button> createTicketsButtons(net.dv8tion.jda.api.entities.User byUser, // jfc
                                                    SupportTicket ticket, int pageIndex, boolean hasNext
    ) {
        boolean hasPrev = pageIndex > 0;

        ButtonContext getMessages = ButtonContext.success("Baixar", Bot.Emojis.DOWNLOAD)
                .setScope(Scopes.Tickets.DOWNLOAD_MESSAGES)
                .put("ticket", ticket);

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Tickets.PAGINATE_TICKETS)
                .put("by_user", byUser)
                .put("page_index", pageIndex - 1)
                .put("ticket", ticket)
                .setEnabled(hasPrev);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Tickets.PAGINATE_TICKETS)
                .put("by_user", byUser)
                .put("page_index", pageIndex + 1)
                .put("ticket", ticket)
                .setEnabled(hasNext);

        INTERACTION_MANAGER.save(prev, next);
        return List.of(getMessages.getEntity(), prev.getEntity(), next.getEntity());
    }

    public static List<Button> createNamesHistoryButtons(NameScope type, long targetId, int currentOffset, boolean hasNext) {
        int previousOffset = currentOffset - 10;
        int nextOffset = currentOffset + 10;
        boolean hasPrevious = currentOffset >= 10;

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Misc.PAGINATE_NAME_UPDATE)
                .put("offset", previousOffset)
                .put("type", type)
                .put("target_id", targetId)
                .setPermission(Permission.MANAGE_SERVER)
                .setEnabled(hasPrevious);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Misc.PAGINATE_NAME_UPDATE)
                .put("offset", nextOffset)
                .put("type", type)
                .put("target_id", targetId)
                .setPermission(Permission.MANAGE_SERVER)
                .setEnabled(hasNext);

        INTERACTION_MANAGER.save(prev, next);
        return List.of(prev.getEntity(), next.getEntity());
    }

    public static List<Button> createProposalsListButtons(int page, long userId, boolean hasNext, String type) {
        int previousPage = page - 1;
        int nextPage = page + 1;
        boolean hasPrevious = page > 1;

        ButtonContext prev = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_LEFT)
                .setScope(Scopes.Misc.PAGINATE_MARRIAGE_REQUESTS)
                .addUser(userId)
                .put("page", previousPage)
                .put("type", type)
                .setEnabled(hasPrevious);

        ButtonContext next = ButtonContext.secondary(Bot.Emojis.GRAY_ARROW_RIGHT)
                .setScope(Scopes.Misc.PAGINATE_MARRIAGE_REQUESTS)
                .addUser(userId)
                .put("page", nextPage)
                .put("type", type)
                .setEnabled(hasNext);

        INTERACTION_MANAGER.save(prev, next);
        return List.of(prev.getEntity(), next.getEntity());
    }

    public static Button createTicTacToeInvite(long authorId, long otherId, int amount) {
        ButtonContext ctx = ButtonContext.success("Aceitar", GAME_EMOJI)
                .setScope(Scopes.Bets.CREATE_TICTACTOE)
                .addUser(otherId)
                .put("author_id", authorId)
                .put("amount", amount);

        INTERACTION_MANAGER.save(ctx);
        return ctx.getEntity();
    }

    public static Button[][] createTicTacToeTable(long id, long current, GameGrid grid) {
        char[][] board = grid.getBoard();
        int size = grid.size();
        Button[][] buttons = new Button[size][size];

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                char value = grid.get(i, j);
                Emoji emoji = GameGrid.EMOJIS.get(value);
                String label = emoji == null ? ZERO_WIDTH_SPACE : null;

                ButtonContext ctx = ButtonContext.secondary(label, emoji)
                        .setScope(Scopes.Bets.TICTACTOE_GAME)
                        .setEnabled(value == 0)
                        .addUser(current)
                        .put("bet_id", id)
                        .put("row", i)
                        .put("col", j);

                INTERACTION_MANAGER.save(ctx);
                buttons[i][j] = ctx.getEntity();
            }
        }
        return buttons;
    }

    public static List<Button> createRemoveColorRoleButtons(
            ColorRoleState state, Role role, User user, boolean hasRefund
    ) {
        int refund = hasRefund ? state.getValuePaid() : 0;
        ButtonContext confirm = ButtonContext.danger("Confirmar Remoção", TRASH_EMOJI)
                .setScope(Scopes.Shop.REMOVE_COLOR_ROLE)
                .addUser(user.getIdLong())
                .put("currency", state.getCurrency())
                .put("user", user)
                .put("refund", refund)
                .put("role", role);

        INTERACTION_MANAGER.save(confirm);
        return List.of(confirm.getEntity());
    }

    public static List<Button> createColorRoleButtons(ColorRoleItem color, Role role, User user) {
        ButtonContext payOfc = ButtonContext.success("Pagar com Oficina", CurrencyType.OFICINA.getEmoji())
                .setScope(Scopes.Shop.ADD_COLOR_ROLE)
                .addUser(user.getIdLong())
                .put("currency", CurrencyType.OFICINA)
                .put("user", user)
                .put("color", color)
                .put("role", role);

        ButtonContext payUnb = ButtonContext.success("Pagar com UnbelievaBoat", CurrencyType.UNBELIEVABOAT.getEmoji())
                .setScope(Scopes.Shop.ADD_COLOR_ROLE)
                .put("currency", CurrencyType.UNBELIEVABOAT)
                .put("user", user)
                .put("color", color)
                .put("role", role);

        INTERACTION_MANAGER.save(payOfc, payUnb);
        return List.of(payOfc.getEntity(), payUnb.getEntity());
    }

    public static Button createGroupChannelConfirm(OficinaGroup group, ChannelType channelType, int price) {
        return createGroupItemPaymentConfirm(
                group,
                Scopes.Group.CREATE_CHANNEL,
                group.getCurrency().getEmoji(),
                price,
                Bot.map("channel_type", channelType)
        );
    }

    public static Button createInvoiceConfirm(OficinaGroup group, int amount) {
        return createGroupItemPaymentConfirm(
                group,
                Scopes.Group.PAY_INVOICE,
                group.getCurrency().getEmoji(),
                amount,
                Bot.map()
        );
    }

    public static Button createPermissionConfirm(OficinaGroup group, GroupPermission perm, int amount) {
        return createGroupItemPaymentConfirm(
                group,
                Scopes.Group.ADD_PERMISSION,
                group.getCurrency().getEmoji(),
                amount,
                Bot.map("permission", perm)
        );
    }

    public static Button createMessagePinConfirm(OficinaGroup group, long messageId, int price) {
        return createGroupItemPaymentConfirm(
                group,
                Scopes.Group.PIN_MESSAGE,
                group.getCurrency().getEmoji(),
                price,
                Bot.map(
                        "message_id", messageId,
                        "is_pin", true,
                        "group", group
                )
        );
    }

    public static Button createMessageUnpinConfirm(OficinaGroup group, long messageId) {
        return createGroupItemRemotionConfirm(
                group,
                Scopes.Group.PIN_MESSAGE,
                Emoji.fromUnicode("🗑"),
                0,
                Bot.map(
                        "message_id", messageId,
                        "is_pin", false,
                        "group", group
                )
        );
    }

    public static Button createModifyGroupConfirm(OficinaGroup group, String newName, int newColor, int price) {
        return createGroupItemPaymentConfirm(
                group,
                Scopes.Group.UPDATE_GROUP,
                group.getCurrency().getEmoji(),
                price,
                Bot.map("new_name", newName, "new_color", newColor)
        );
    }

    public static Button createGroupBotAddConfirm(OficinaGroup group, GroupBot bot, int price) {
        return createGroupItemPaymentConfirm(
                group,
                Scopes.Group.ADD_BOT,
                group.getCurrency().getEmoji(),
                price,
                Bot.map("bot", bot)
        );
    }

    public static Button createGroupConfirm(OficinaGroup partialGroup, int color) {
        return createGroupItemPaymentConfirm(
                partialGroup,
                Scopes.Group.CREATE_GROUP,
                partialGroup.getCurrency().getEmoji(),
                partialGroup.getAmountPaid(),
                Bot.map("group_color", color)
        );
    }

    public static Button createAddGroupMemberConfirm(OficinaGroup group, Member newMember, int price) {
        return createGroupItemPaymentConfirm(
                group,
                Scopes.Group.ADD_MEMBER,
                group.getCurrency().getEmoji(),
                price,
                Bot.map("new_member", newMember)
        );
    }

    public static Button createRemoveGroupMemberConfirm(OficinaGroup group, long targetId) {
        return createGroupItemRemotionConfirm(
                group,
                Scopes.Group.REMOVE_MEMBER,
                null,
                0,
                Bot.map("target_id", targetId)
        );
    }

    /* -------------------- Modals -------------------- */
    public static Modal createChoosableRolesModal(Message.Attachment img, long chanId, int color, int maxChoices) {
        int maxTitle = MessageEmbed.AUTHOR_MAX_LENGTH;
        int maxDesc = TextInput.MAX_VALUE_LENGTH;
        TextInputStyle shortStyle = TextInputStyle.SHORT;
        TextInputStyle textStyle = TextInputStyle.PARAGRAPH;

        List<Label> labels = List.of(
                Label.of("Título", TextInput.create("title", shortStyle)
                        .setMaxLength(maxTitle)
                        .build()),

                Label.of("Descrição", TextInput.create("desc", textStyle)
                        .setMaxLength(maxDesc)
                        .setRequired(false)
                        .build()),

                Label.of("Opções", TextInput.create("opts", textStyle).build())
        );
        ModalContext ctx = ModalContext.of("Choosable Roles", labels)
                .setScope(Scopes.Misc.CHOOSABLE_ROLES)
                .put("image", img)
                .put("channel_id", chanId)
                .put("color", color)
                .put("max_choices", maxChoices);

        INTERACTION_MANAGER.save(ctx);
        return ctx.getEntity();
    }

    public static Modal createTicketModal() {
        List<Label> labels = List.of(
                Label.of(
                        "Assunto",
                        "Um breve título/descrição da ocorrência.",
                        TextInput.create("subject", toStyle(true))
                                .setRequiredRange(5, SupportTicket.MAX_SUBJECT_LENGTH)
                                .build()),

                Label.of(
                        "Descrição",
                        "Você poderá adicionar mais detalhes após a criação do ticket.",
                        TextInput.create("body", toStyle(false))
                                .setRequiredRange(10, SupportTicket.MAX_BODY_LENGTH)
                                .build())
        );

        ModalContext ctx = ModalContext.of("🎫 Novo Ticket", labels)
                .setScope(Scopes.Tickets.CREATE_TICKET);

        INTERACTION_MANAGER.save(ctx);
        return ctx.getEntity();
    }

    public static Modal createTicketCloseModal() {
        final String reason = "Resolvido com sucesso.";
        Label label = Label.of("Motivo", TextInput.create("reason", toStyle(false))
                .setRequiredRange(5, 2000)
                .setValue(reason)
                .build());
        ModalContext ctx = ModalContext.of("Fechar Ticket", label)
                .setScope(Scopes.Tickets.DELETE_TICKET);

        INTERACTION_MANAGER.save(ctx);
        return ctx.getEntity();
    }

    // Utility Internals
    private static Button createGroupItemPaymentConfirm(
            OficinaGroup group, String scope, Emoji emoji, int price, Map<String, Object> payload
    ) {
        return genericConfirmButton(group, ButtonStyle.SUCCESS, "Pagamento", emoji, scope, price, payload);
    }

    private static Button createGroupItemRemotionConfirm(
            OficinaGroup group, String scope, Emoji emoji, int price, Map<String, Object> payload
    ) {
        return genericConfirmButton(group, ButtonStyle.DANGER, "Remoção", emoji, scope, price, payload);
    }

    private static TextInputStyle toStyle(boolean isShort) {
        return isShort ? TextInputStyle.SHORT : TextInputStyle.PARAGRAPH;
    }

    private static Button genericConfirmButton(
            OficinaGroup group, ButtonStyle style, String act, Emoji emoji,
            String scope, int price, Map<String, Object> payload
    ) {
        String label = String.format("Confirmar %s", act);
        ButtonContext confirm = ButtonContext.of(style, label, emoji)
                .addUser(group.getOwnerId())
                .setScope(scope)
                .setValidity(30, TimeUnit.SECONDS)
                .put("group", group)
                .put("amount", price)
                .putAll(payload == null ? Map.of() : payload);

        INTERACTION_MANAGER.save(confirm);
        return confirm.getEntity();
    }
}