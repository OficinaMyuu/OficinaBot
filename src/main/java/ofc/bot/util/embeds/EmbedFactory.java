package ofc.bot.util.embeds;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.internal.utils.Checks;
import net.redhogs.cronparser.CronExpressionDescriptor;
import ofc.bot.commands.slash.economy.LeaderboardCommand;
import ofc.bot.domain.entity.*;
import ofc.bot.domain.entity.enums.*;
import ofc.bot.domain.viewmodels.*;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.twitch.TwitchService;
import ofc.bot.util.Bot;
import ofc.bot.util.OficinaEmbed;

import java.awt.*;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for embeds used in multiple classes.
 * <p>
 * If an embed is used in a single command, with no pagination or confirmation
 * system, then the {@code embed()} method will remain in the same class.
 */
public final class EmbedFactory {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String HYPERLINK_FORMAT = "[%s](<%s>)"; // We surround with <> to hide additional embeds
    public static final Color DANGER_RED = new Color(255, 50, 50);
    public static final Color OK_GREEN = new Color(80, 255, 80);

    private EmbedFactory() {}

    public static MessageEmbed embedBirthdayList(List<Birthday> birthdays, Guild guild, Month month) {
        EmbedBuilder builder = new EmbedBuilder();
        String monthName = getMonthDisplay(month);
        String formattedBirthdays = formatBirthdays(birthdays);
        boolean empty = birthdays.isEmpty();

        return builder
                .setAuthor("Aniversários", null, Birthday.ICON_URL)
                .setDescription("## " + monthName + "\n\n" + (empty ? "*Nenhum aniversariante.*" : formattedBirthdays))
                .setColor(Bot.Colors.DISCORD)
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedPrivilegedBirthday(User user, Birthday birthday) {
        EmbedBuilder builder = new EmbedBuilder();
        String desc = String.format("O Davizinho faz aniversáriozinho dia %s.", birthday.getPrettyBirthday());
        double mediocreChance = Bot.getMediocreChance();

        return builder
                .setAuthor("Davizinho", null, user.getEffectiveAvatarUrl())
                .setDescription(desc)
                .setColor(new Color(182, 88, 236))
                .setFooter(String.format("Juan: %,.2f%%/60%%", mediocreChance))
                .build();
    }

    public static MessageEmbed embedChoosableRoles(Guild guild, String title, String desc, String filename, int color) {
        OficinaEmbed builder = new OficinaEmbed();
        Color embedColor = color == 0 ? Bot.Colors.DEFAULT : new Color(color);

        return builder
                .setAuthor(title, null, guild.getIconUrl())
                .setDesc(desc)
                .setColor(embedColor)
                .setImageIf(filename != null, "attachment://" + filename)
                .build();
    }

    public static MessageEmbed embedTransactions(User user, Guild guild, PageItem<BankTransaction> trs) {
        OficinaEmbed builder = new OficinaEmbed();
        String title = String.format("Transações de %s", user.getEffectiveName());
        String resultsFound = String.format("Resultados encontrados: `%s`.", Bot.fmtNum(trs.getRowCount()));
        String pages = String.format("Pág. %s/%s", Bot.fmtNum(trs.getPage()), Bot.fmtNum(trs.getPageCount()));

        return builder
                .setAuthor(title, null, user.getEffectiveAvatarUrl())
                .setColor(Bot.Colors.DEFAULT)
                .setDesc(resultsFound)
                .appendDescription(formatTransactions(trs.getEntities()))
                .setFooter(pages, guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedInfractions(
            User user, Guild guild, PageItem<MemberPunishment> infrs, long moderatorId
    ) {
        OficinaEmbed builder = new OficinaEmbed();
        MemberPunishment infr = infrs.get(0);
        boolean active = infr.isActive();
        String modMention = String.format("<@%d>", moderatorId);
        String infrCreation = String.format("<t:%d>", infr.getTimeCreated());
        String resultsFound = String.format("Resultados encontrados: `%s`.", Bot.fmtNum(infrs.getRowCount()));
        String pages = String.format("Pág. %s/%s", Bot.fmtNum(infrs.getPage()), Bot.fmtNum(infrs.getPageCount()));
        String delAuthorMention = Bot.ifNull(infr.getDeletionAuthorMention(), "Ninguém");

        return builder
                .setAuthor(user.getEffectiveName(), null, user.getAvatarUrl())
                .setColor(Bot.Colors.DEFAULT)
                .setDesc(resultsFound)
                .addField("👑 Moderador", modMention)
                .addField("📅 Punido em", infrCreation)
                .addField("📌 Ativo", active ? "Sim" : "Não")
                .addFieldIf(!active, "🚫 Removido por", delAuthorMention)
                .addField("📖 Motivo", infr.getReason(), false)
                .setFooter(pages, guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedReminder(User user, Reminder rem) {
        OficinaEmbed builder = new OficinaEmbed();

        int times = rem.getTriggerTimes();
        int timesLeft = rem.getTriggersLeft();
        long timeCreated = rem.getTimeCreated();
        long lastTrigger = rem.getLastTimeTriggered();
        ChannelType chanType = rem.getChannelType();
        String channel = chanType ==  ChannelType.PRIVATE ? "Privado" : String.format("<#%d>", rem.getChannelId());
        String head = String.format("Lembrete de %s", user.getEffectiveName());
        String message = rem.getMessage();
        String execDescription = formatReminderValue(rem);
        String execTimes = times == 1 ? "1 vez" : times + " vezes";
        String execTimesLeft = timesLeft == 1 ? "1 execução" : timesLeft + " execuções";
        String execLastTime = String.format("<t:%d:F>", lastTrigger);
        Color color = rem.isExpired() ? DANGER_RED : Bot.Colors.DEFAULT;
        ReminderType type = rem.getType();

        return builder
                .setAuthor(head, null, user.getEffectiveAvatarUrl())
                .setDesc(message)
                .setColor(color)
                .addField("📖 Canal", channel, true)
                .addField("🎈 Tipo", type.getName(), true)
                .addField("⏰ Lembrete", execDescription, true)
                .addFieldIf(times != -1, "⚙️ Execuções", execTimes, true)
                .addFieldIf(lastTrigger > 0, "⌛ Última Execução", execLastTime)
                .addFieldIf(timesLeft > 0, "🕒 Restam", execTimesLeft)
                .setTimestamp(Instant.ofEpochSecond(timeCreated))
                .build();
    }
    
    public static MessageEmbed embedReminderDeleted(User user) {
        EmbedBuilder builder = new EmbedBuilder();
        
        return builder
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setColor(DANGER_RED)
                .setDescription("🗑 Lembrete apagado com sucesso!")
                .build();
    }

    public static MessageEmbed embedReminderTrigger(String message, long timeCreated) {
        EmbedBuilder builder = new EmbedBuilder();

        return builder
                .setTitle("Lembrete")
                .setColor(Bot.Colors.DEFAULT)
                .setDescription(message)
                .setTimestamp(Instant.ofEpochSecond(timeCreated))
                .build();
    }

    public static MessageEmbed embedAtReminder(User user, ZonedDateTime moment) {
        OficinaEmbed builder = new OficinaEmbed();
        long epoch = moment.toInstant().getEpochSecond();

        return builder
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setColor(OK_GREEN)
                .setDescf("Lembrarei você em <t:%d:F>.", epoch)
                .build();
    }

    public static MessageEmbed embedCronReminder(User user, String expression) {
        OficinaEmbed builder = new OficinaEmbed();

        return builder
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setColor(OK_GREEN)
                .setDescf("Lembrarei você baseado na expressão: `%s`.", expression)
                .build();
    }

    public static MessageEmbed embedPeriodicReminder(User user, long period, int repeat) {
        OficinaEmbed builder = new OficinaEmbed();
        int times = repeat + 1;
        String timesText = times == 1 ? "vez" : "vezes";
        String fmtPeriod = Bot.parsePeriod(period);

        return builder
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setColor(OK_GREEN)
                .setDescfIf(times == 1, "Lembrarei você em %s.", fmtPeriod)
                .setDescfIf(times != 1, "Lembrarei você %d %s a cada %s.", times, timesText, fmtPeriod)
                .build();
    }

    public static MessageEmbed embedLeaderboard(Guild guild, PageItem<LeaderboardUser> lb) {
        EmbedBuilder builder = new EmbedBuilder();

        int page = lb.getPage();
        String pages = String.format("Pág %s/%s", Bot.fmtNum(page), Bot.fmtNum(lb.getPageCount()));
        List<LeaderboardUser> users = lb.getEntities();

        return builder
                .setAuthor("Economy Leaderboard", null, UserEconomy.BANK_ICON)
                .setDescription("💸 Placar de Líderes Global.\n\n" + formatLeaderboardUsers(users, page))
                .setColor(Bot.Colors.DEFAULT)
                .setFooter(pages, guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedLevels(Guild guild, LevelView user, PageItem<LevelView> levels) {
        EmbedBuilder builder = new EmbedBuilder();

        int page = levels.getPage();
        String fmtPages = String.format("Pág %s/%s", Bot.fmtNum(page), Bot.fmtNum(levels.getPageCount()));
        String userRow = String.format(UserXP.LEADERBOARD_ROW_FORMAT, user.rank(), user.displayIdentifier(), Bot.humanizeNum(user.level()));
        
        return builder
                .setAuthor("Levels Leaderboard - Global", null, guild.getIconUrl())
                .setDescription("📊 Placar global de níveis.\n\n" + formatLevelUsers(levels))
                .appendDescription("\n\n")
                .appendDescription(userRow)
                .setColor(Bot.Colors.DEFAULT)
                .setFooter(fmtPages)
                .build();
    }

    public static MessageEmbed embedUsernameUpdates(NamesHistoryView namesHistoryDTO, Guild guild, User target) {
        EmbedBuilder builder = new EmbedBuilder();

        List<UserNameUpdate> names = namesHistoryDTO.names();
        String name = target.getEffectiveName();
        String avatar = target.getEffectiveAvatarUrl();
        String page = Bot.fmtNum(namesHistoryDTO.page());
        String maxPages = Bot.fmtNum(namesHistoryDTO.maxPages());
        String results = Bot.fmtNum(namesHistoryDTO.total());
        String description = String.format("Resultados encontrados: `%s`.\n\n%s", results, formatUsernameUpdates(names));

        builder.setAuthor(name, null, avatar)
                .setDescription(description)
                .setColor(Color.CYAN)
                .setFooter(page + "/" + maxPages, guild.getIconUrl());

        return builder.build();
    }

    public static MessageEmbed embedProposals(Guild guild, User user, ProposalsView proposals) {
        EmbedBuilder builder = new EmbedBuilder();
        String type = proposals.type();
        String prettyType = "in".equals(type)
                ? "recebidas"
                : "enviadas";

        int page = proposals.page();
        int maxPages = proposals.maxPages();
        List<MarriageRequest> users = proposals.requests();
        String pages = String.format("Pág %s/%s", Bot.fmtNum(page), Bot.fmtNum(maxPages));
        String prettyCount = Bot.fmtNum(proposals.requestCount());
        String desc = String.format("Propostas de casamento **%s**: `%s`.\n\n%s", prettyType, prettyCount, formatProposals(users, type));

        return builder
                .setAuthor(user.getEffectiveName(), null, user.getEffectiveAvatarUrl())
                .setDescription(desc)
                .setColor(Bot.Colors.DEFAULT)
                .setFooter(pages, guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedPunishment(User user, PunishmentType action, String reason, long duration) {
        OficinaEmbed embed = new OficinaEmbed();
        String header = String.format("%s foi %s", user.getName(), action.getDisplay());

        return embed
                .setAuthor(header, null, user.getEffectiveAvatarUrl())
                .setColor(Bot.Colors.DEFAULT)
                .appendDescriptionIf(reason != null, "**Motivo:** " + reason + "\n\n")
                .appendDescriptionIf(duration > 0, "**Duração:** " + Bot.parsePeriod(duration))
                .build();
    }

    public static MessageEmbed embedPunishment(User user, PunishmentType action, String reason) {
        return embedPunishment(user, action, reason, 0);
    }

    public static MessageEmbed embedBankAction(User user, Color color, String format, Object... args) {
        EmbedBuilder builder = new EmbedBuilder();

        return builder
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setColor(color)
                .setDescription(String.format(format, args))
                .build();
    }

    public static MessageEmbed embedTicTacToeCreate(Guild guild, User author, User other, int amount) {
        EmbedBuilder builder = new EmbedBuilder();
        String head = String.format("%s quer jogar Jogo da velha", author.getEffectiveName());

        return builder
                .setAuthor(head, null, author.getEffectiveAvatarUrl())
                .setColor(Bot.Colors.DEFAULT)
                .setThumbnail(other.getEffectiveAvatarUrl())
                .addField("💰 Aposta", Bot.fmtNum(amount), true)
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedTicTacToeEnd(User winner) {
        EmbedBuilder builder = new EmbedBuilder();
        boolean draw = winner == null;
        Color color = draw ? Color.YELLOW : Color.GREEN;
        String name = draw ? "Deu velha!" : winner.getEffectiveName() + " venceu! 🥳";
        String url = draw ? null : winner.getEffectiveAvatarUrl();

        return builder
                .setAuthor(name, null, url)
                .setColor(color)
                .build();
    }

    public static MessageEmbed embedTicTacToeTimeout(int amount, User penalized) {
        OficinaEmbed builder = new OficinaEmbed();
        String head = String.format("%s foi penalizado!", penalized.getEffectiveName());

        return builder
                .setAuthor(head, null, penalized.getEffectiveAvatarUrl())
                .appendDescf("⚠️ Penalizado em %s por inatividade.", Bot.fmtMoney(amount))
                .setColor(DANGER_RED)
                .build();
    }

    public static MessageEmbed embedTicTacToeDeleted(User author, Guild guild) {
        OficinaEmbed builder = new OficinaEmbed();

        return builder
                .setColor(DANGER_RED)
                .setThumbnail(author.getEffectiveAvatarUrl())
                .setDescf("""
                        ## ❌ Jogo da Velha finalizado forçadamente
                        > É esperado que os membros da equipe do servidor ajam com responsabilidade \
                        ao utilizar recursos que impactam a economia do servidor. \
                        No entanto, diante da situação ocorrida, o membro %s foi penalizado com a \
                        **suspensão total** do acesso a este bot por um período de `7 dias`.

                        > Inclui-se, mas não se limita também à perda total dos ganhos de XP durante esse período.

                        ⚠️ **O ban __NÃO SERÁ REMOVIDO__ em casos de identificação de conduta indevida.**

                        Esperamos uma postura mais responsável no futuro.
                        """, author.getAsMention())
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedRateLimited(User user, long nextAvailable) {
        OficinaEmbed builder = new OficinaEmbed();

        return builder
                .setAuthor(user.getName(), null, user.getEffectiveAvatarUrl())
                .setDescf("\uD83D\uDD52 Você poderá usar este comando novamente <t:%d:R>.", nextAvailable)
                .setColor(DANGER_RED)
                .build();
    }

    public static MessageEmbed embedTicTacToeGame(User current) {
        EmbedBuilder builder = new EmbedBuilder();
        String name = "Vez de " + current.getEffectiveName();
        String url = current.getEffectiveAvatarUrl();

        return builder
                .setAuthor(name, null, url)
                .setColor(Bot.Colors.DEFAULT)
                .build();
    }

    public static MessageEmbed embedGroupChannelCreate(
            Member buyer, OficinaGroup group, ChannelType type, int price
    ) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                buyer.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja confirmar a compra deste canal?",
                price,
                Map.of("📚 Tipo", Bot.upperFirst(type.name().toLowerCase()))
        );
    }

    public static MessageEmbed embedInvoicePayment(Member buyer, OficinaGroup group, int amount) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                buyer.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja confirmar o pagamento da fatura?",
                amount,
                Bot.map()
        );
    }

    public static MessageEmbed embedGroupPermissionAdd(
            Member buyer, OficinaGroup group, GroupPermission perm, int amount
    ) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                buyer.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja confirmar a adição desta permissão?",
                amount,
                Bot.map("\uD83D\uDC6E Permissão", perm.getDisplay())
        );
    }

    public static MessageEmbed embedGroupMemberAdd(Member buyer, OficinaGroup group, Member newMember, int price) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                newMember.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja confirmar a adição deste membro?",
                price,
                Map.of("👤 Membro", newMember.getAsMention())
        );
    }

    public static MessageEmbed embedGroupMemberRemove(Member buyer, OficinaGroup group, Member member) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                member.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja confirmar a remoção deste membro?",
                0,
                Map.of("👤 Membro", member.getAsMention())
        );
    }

    public static MessageEmbed embedGroupCreate(Member buyer, OficinaGroup group, int color) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                buyer.getUser().getEffectiveAvatarUrl(),
                color,
                "Deseja confirmar a compra deste grupo?",
                group.getAmountPaid(),
                Map.of("🎨 Cor", Bot.fmtColorHex(color))
        );
    }

    public static MessageEmbed embedGroupMessagePin(Member buyer, OficinaGroup group, String messageUrl, int price) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                buyer.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja fixar esta mensagem?",
                price,
                Bot.map("📖 Mensagem", messageUrl)
        );
    }

    public static MessageEmbed embedGroupMessageUnpin(Member owner, OficinaGroup group, String messageUrl) {
        return embedGroupSellConfirmation(
                owner,
                group,
                owner.getUser().getEffectiveAvatarUrl(),
                DANGER_RED.getRGB(),
                "Deseja desfixar esta mensagem?",
                0,
                Bot.map("📖 Mensagem", messageUrl)
        );
    }

    public static MessageEmbed embedGroupBotAdd(Member buyer, OficinaGroup group, GroupBot bot, int price) {
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                buyer.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja confirmar a adição deste bot?",
                price,
                Map.of("🤖 Bot", bot.getBotMention())
        );
    }

    public static MessageEmbed embedGroupModify(
            Member buyer, OficinaGroup group, String newName, int newColor, int price
    ) {
        StringBuilder itemsList = new StringBuilder();
        if (newName != null) itemsList.append("Nome").append("\n");
        if (newColor != -1) itemsList.append("Cor");
        return embedGroupPurchaseConfirmation(
                buyer,
                group,
                buyer.getUser().getEffectiveAvatarUrl(),
                null,
                "Deseja confirmar a modificação deste grupo?",
                price,
                Map.of("🎈 Modificações", itemsList.toString())
        );
    }

    public static MessageEmbed embedCreatedTicket(Member issuer, String subject, String body) {
        EmbedBuilder builder = new EmbedBuilder();
        Guild guild = issuer.getGuild();
        String desc = String.format("### %s\n\n%s", subject, body);

        return builder
                .setTitle("Dados do Ticket")
                .setColor(Bot.Colors.DEFAULT)
                .setDescription(desc)
                .setThumbnail(issuer.getEffectiveAvatarUrl())
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }

    public static MessageEmbed embedTicketPage(User issuer, Guild guild, SupportTicket ticket, Set<Long> users) {
        OficinaEmbed builder = new OficinaEmbed();
        long ticketChanId = ticket.getChannelId();
        Instant timestamp = Instant.ofEpochSecond(ticket.getTimeCreated());
        TextChannel ticketChan = guild.getTextChannelById(ticketChanId);
        boolean isOpen = ticketChan != null;
        String fmtClosureAuthor = String.format("<@%d>", ticket.getClosedAuthorId());
        String fmtStatus = isOpen ? "Aberto" : "Fechado";
        String fmtUsers = users.stream().map(l -> String.format("- <@%d>", l)).collect(Collectors.joining("\n"));

        return builder
                .setAuthor(ticket.getTitle())
                .setColor(Bot.Colors.DEFAULT)
                .setDesc(ticket.getDescription())
                .setThumbnail(issuer.getEffectiveAvatarUrl())
                .addField("🎈 Autor", String.format("<@%d>", ticket.getInitiatorId()))
                .addField("🏷 Status", fmtStatus)
                .addFieldIf(!isOpen, "🚩 Quem Fechou", fmtClosureAuthor)
                .addFieldIf(!isOpen, "📜 Por Que Fechou", ticket.getCloseReason())
                .addFieldIf(isOpen, "📚 Canal", String.format("<#%d>", ticketChanId))
                .addField("👥 Envolvidos", fmtUsers, false)
                .setFooter(guild.getName(), guild.getIconUrl())
                .setTimestamp(timestamp)
                .build();
    }

    private static MessageEmbed embedGroupSellConfirmation(
            Member member, OficinaGroup group, String thumbUrl,
            Integer color, String act, int refund, Map<String, Object> fields
    ) {
        int embedColor = color == null ? group.resolveColor() : color;
        Map<String, Object> fieldsMap = new LinkedHashMap<>();
        fieldsMap.put("💰 Reembolso", Bot.fmtMoney(refund));
        fieldsMap.putAll(fields);
        return embedGroupConfirmation(
                member, group, thumbUrl, act, embedColor, fieldsMap
        );
    }

    private static MessageEmbed embedGroupPurchaseConfirmation(
            Member buyer, OficinaGroup group, String thumbUrl,
            Integer color, String act, int price, Map<String, Object> fields
    ) {
        int embedColor = color == null ? group.resolveColor() : color;
        Map<String, Object> fieldsMap = new LinkedHashMap<>();
        fieldsMap.put("💰 Valor", Bot.fmtMoney(price));
        fieldsMap.putAll(fields);
        return embedGroupConfirmation(
                buyer, group, thumbUrl, act, embedColor, fieldsMap
        );
    }

    private static MessageEmbed embedGroupConfirmation(
            Member member, OficinaGroup group, String thumbUrl, String act,
            int color, Map<String, Object> fields
    ) {
        EmbedBuilder builder = new EmbedBuilder();
        Guild guild = member.getGuild();
        CurrencyType currency = group.getCurrency();
        String guildName = guild.getName();
        String groupName = group.getName();

        builder
                .setTitle(groupName)
                .setDescription(act)
                .setThumbnail(thumbUrl)
                .setColor(color)
                .setFooter(guildName, guild.getIconUrl())
                .addField("💳 Economia", currency.getName(), true);

        fields.forEach((k, v) -> builder.addField(k, v.toString(), true));
        return builder.build();
    }

    private static String formatTransactions(List<BankTransaction> trs) {
        TransactionEntryBuilder builder = new TransactionEntryBuilder();

        builder.addSeparator();
        for (BankTransaction tr : trs) {
            long amount = tr.getAmount();
            CurrencyType currency = tr.getCurrencyType();
            TransactionType action = tr.getAction();
            AppUser user = tr.retrieveUser();
            AppUser receiver = tr.retrieveReceiver();
            LocalDateTime timestamp = LocalDateTime.ofEpochSecond(tr.getTimeCreated(), 0, ZoneOffset.ofHours(-3));
            String fmtTimestamp = timestamp.format(DATE_TIME_FORMATTER);
            String recName = receiver == null ? null : receiver.getName();
            String comment = Bot.ifNull(tr.getComment(), "--");
            StoreItemType product = tr.getProduct();
            String productName = product == null ? null : product.getName();

            builder.addF("ID: #%d | %s (GMT -3)", tr.getId(), fmtTimestamp)
                    .addF("Tipo: %s", action.getName())
                    .addFIf(product != null, "Item: %s", productName)
                    .addF("%s: %s", resolveUserAlias(action), user.getName())
                    .addFIf(receiver != null, "Recebente: %s", recName)
                    .addF("Nota: %s", comment)
                    .addF("Valor: %s (%s)", Bot.fmtMoney(amount), currency.getName())
                    .addSeparator();
        }
        return builder.build();
    }

    private static String resolveUserAlias(TransactionType action) {
        return switch (action) {
            case FEE_PAID,
                 INVOICE_PAID -> "Pagador";
            case CHAT_MONEY,
                 WORK_EXECUTED,
                 BET_RESULT,
                 BET_PENALTY,
                 MARRIAGE_CREATED,
                 DAILY_COLLECTED -> "Membro";
            case AMOUNT_ROBBED -> "Assaltante";
            case AMOUNT_FINED -> "Multado";
            case BALANCE_SET,
                 BALANCE_UPDATED -> "Moderador";
            case ITEM_BOUGHT -> "Comprador";
            case ITEM_SOLD -> "Vendedor";
            case MONEY_TRANSFERRED -> "Remetente";
        };
    }

    private static String formatProposals(List<MarriageRequest> requests, String type) {
        return Bot.format(requests, (req) -> {
            long timestamp = req.getTimeCreated();

            return switch (type) {
                case "in" -> String.format("- <@%d> (<t:%d:D>)\n", req.getRequesterId(), timestamp);
                case "out" -> String.format("- <@%d> (<t:%d:D>)\n", req.getTargetId(), timestamp);

                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
        });
    }

    private static String formatUsernameUpdates(List<UserNameUpdate> names) {
        return Bot.format(names, (update) -> {
            String value = update.getNewValue();
            String name = value == null ? "*removed*" : escapeUsernameSpecialChars(value);
            long timestamp = update.getTimeCreated();

            return switch (update.getScope()) {
                case USERNAME, GLOBAL_NAME -> String.format("- <t:%d:d> %s\n", timestamp, name);

                case GUILD_NICK -> String.format("- <t:%d:d> %s <@%d>\n", timestamp, name, update.getAuthorId());
            };
        });
    }

    private static String escapeUsernameSpecialChars(String name) {
        return name
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`");
    }

    private static String formatBirthdays(List<Birthday> birthdays) {
        return Bot.format(
                birthdays,
                (b) -> String.format(Birthday.BIRTHDAYS_FORMAT, b.getPrettyBirthday(), b.getUserId())
        ).strip();
    }

    private static String formatLevelUsers(PageItem<LevelView> levels) {
        StringBuilder builder = new StringBuilder();
        List<LevelView> entities = levels.getEntities();
        int offset = levels.getOffset();
        int pos = 1;

        for (LevelView user : entities) {
            int itemPos = offset + pos++;
            String row = String.format(
                    UserXP.LEADERBOARD_ROW_FORMAT,
                    itemPos, user.displayIdentifier(), Bot.humanizeNum(user.level())
            );
            builder.append(row).append("\n");
        }
        return builder.toString().strip();
    }

    private static String formatLeaderboardUsers(List<LeaderboardUser> users, int page) {
        StringBuilder builder = new StringBuilder();
        int offset = (page - 1) * LeaderboardCommand.PAGE_SIZE;
        int pos = 1;

        for (LeaderboardUser er : users) {
            int itemPos = offset + pos++;
            String row = String.format(
                    UserEconomy.LEADERBOARD_ROW_FORMAT,
                    itemPos, er.displayIdentifier(), Bot.fmtNum(er.balance())
            );

            builder.append(row).append("\n");
        }

        return builder.toString().strip();
    }

    private static String getMonthDisplay(Month month) {
        String rawDisplay = month.getDisplayName(TextStyle.FULL, Bot.defaultLocale());
        return Bot.upperFirst(rawDisplay);
    }

    private static String formatReminderValue(Reminder rem) {
        return switch (rem.getType()) {
            case AT -> String.format("<t:%d:F>", rem.getReminderValue());
            case PERIOD -> String.format("A cada %s", Bot.parsePeriod(rem.getReminderValue()));
            case CRON -> String.format("`%s`\n> %s", rem.getExpression(), describeExpression(rem.getExpression()));
        };
    }

    private static String describeExpression(String exp) {
        if (exp == null || exp.isBlank()) return null;
        try {
            return CronExpressionDescriptor.getDescription(exp, Bot.defaultLocale()) + '.';
        } catch (ParseException e) {
            return null;
        }
    }

    public static MessageEmbed embedTwitchUser(Guild guild, com.github.twitch4j.helix.domain.User user) {
        EmbedBuilder builder = new EmbedBuilder();

        BroadcasterType broadcasterType = getBroadcasterType(user);
        TwitchUserType userType = getTwitchUserType(user);
        String displayName = user.getDisplayName();
        String login = user.getLogin();
        String description = user.getDescription();
        String offlineImageUrl = nullIfBlank(user.getOfflineImageUrl());
        String profileUrl = nullIfBlank(user.getProfileImageUrl());
        String type = String.format("%s / %s", broadcasterType.toHyperlink(), userType.display);
        String id = user.getId();
        String createdAt = String.format("<t:%d>", user.getCreatedAt().getEpochSecond());
        String url = String.format("%s/%s", TwitchService.BASE_URL, login);

        return builder
                .setTitle(displayName)
                .setDescription(description)
                .setColor(Bot.Colors.TWITCH)
                .setThumbnail(profileUrl)
                .setImage(offlineImageUrl)
                .addField("🌐 ID", id, true)
                .addField("🎯 Tipo de Streamer", type, true)
                .addField("📅 Entrou", createdAt, true)
                .addField("🔗 Url", url, true)
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }

    private static String nullIfBlank(String str) {
        return str == null || str.isBlank() ? null : str;
    }

    private static BroadcasterType getBroadcasterType(com.github.twitch4j.helix.domain.User user) {
        String type = user.getBroadcasterType().toUpperCase();
        return BroadcasterType.fromName(type);
    }

    private static TwitchUserType getTwitchUserType(com.github.twitch4j.helix.domain.User user) {
        String type = user.getType().toUpperCase();
        return TwitchUserType.fromName(type);
    }

    private static final class TransactionEntryBuilder {
        private final List<String> fields = new ArrayList<>();

        TransactionEntryBuilder addSeparator() {
            return addF("---------------------------------------------");
        }

        TransactionEntryBuilder addF(String format, Object... args) {
            this.fields.add(String.format(format, args));
            return this;
        }

        TransactionEntryBuilder addFIf(boolean cond, String format, Object... args) {
            if (cond) {
                addF(format, args);
            }
            return this;
        }

        String build() {
            return String.format("```yml\n%s\n```", String.join("\n", fields)).strip();
        }
    }

    private enum BroadcasterType {
        AFFILIATE("Affiliate", "https://help.twitch.tv/s/article/joining-the-affiliate-program"),
        PARTNER("Partner", "https://help.twitch.tv/s/article/partner-program-overview"),
        NORMAL("Normal", null);

        private final String display;
        private final String url;

        BroadcasterType(String display, String url) {
            Checks.notNull(display, "Display");
            this.display = display;
            this.url = url;
        }

        static BroadcasterType fromName(String name) {
            for (BroadcasterType bt : BroadcasterType.values()) {
                if (bt.name().equals(name)) {
                    return bt;
                }
            }
            return NORMAL; // Falls back to a normal user
        }

        String toHyperlink() {
            return this.url == null
                    ? this.display
                    : String.format(HYPERLINK_FORMAT, this.display, this.url);
        }
    }

    private enum TwitchUserType {
        ADMIN("Admin"),
        GLOBAL_MOD("Moderador Global"),
        STAFF("Twitch Staff"),
        NORMAL("Normal");

        private final String display;

        TwitchUserType(String display) {
            this.display = display;
        }

        static TwitchUserType fromName(String name) {
            for (TwitchUserType bt : TwitchUserType.values()) {
                if (bt.name().equals(name)) {
                    return bt;
                }
            }
            return NORMAL;
        }
    }
}