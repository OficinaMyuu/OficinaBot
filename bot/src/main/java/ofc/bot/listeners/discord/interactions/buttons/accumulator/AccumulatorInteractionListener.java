package ofc.bot.listeners.discord.interactions.buttons.accumulator;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.database.repository.AccumulatorPrizeRepository;
import ofc.bot.domain.database.repository.ColorRoleItemRepository;
import ofc.bot.handlers.accumulator.AccumulatorApprovalReport;
import ofc.bot.handlers.accumulator.AccumulatorMessageFactory;
import ofc.bot.handlers.accumulator.AccumulatorPayoutService;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.paginations.PageItem;
import ofc.bot.handlers.paginations.Paginator;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@DiscordEventHandler
public class AccumulatorInteractionListener extends ListenerAdapter {
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final String PRIZE_NOT_FOUND = "Prêmio não encontrado ou já processado.";
    private static final String UPDATE_PRIZE_TITLE = "Não foi possível atualizar o prêmio";
    private static final String REJECT_PRIZE_TITLE = "Não foi possível rejeitar o prêmio";
    private static final String CONFIGURE_PERMISSION_MESSAGE =
            "Só quem adicionou este prêmio ou usuários com Gerenciar Servidor podem alterá-lo.";
    private static final String REJECT_PERMISSION_MESSAGE =
            "Só quem adicionou este prêmio ou usuários com Gerenciar Servidor podem rejeitá-lo.";

    private final AccumulatorPrizeRepository prizeRepo;
    private final ColorRoleItemRepository colorItemRepo;
    private final AccumulatorPayoutService payoutService;

    public AccumulatorInteractionListener(
            AccumulatorPrizeRepository prizeRepo,
            ColorRoleItemRepository colorItemRepo,
            AccumulatorPayoutService payoutService
    ) {
        this.prizeRepo = prizeRepo;
        this.colorItemRepo = colorItemRepo;
        this.payoutService = payoutService;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String customId = event.getComponentId();
        if (!AccumulatorMessageFactory.isAccumulatorId(customId)) {
            return;
        }

        GuildContext context = context(event);
        if (context == null) {
            return;
        }

        String[] parts = AccumulatorMessageFactory.parts(customId);
        switch (action(parts)) {
            case "page" -> handlePage(event, context.member(), parts);
            case "currency" -> handleCurrency(event, context.guild(), context.member(), parts);
            case "color" -> handleColorPrompt(event, context.guild(), context.member(), parts);
            case "reject" -> handleReject(event, context.guild(), context.member(), parts);
            case "pay" -> handlePay(event, context.guild(), context.member(), parts);
            case "all" -> handleApproveAll(event, context.guild(), context.member(), parts);
            default -> event.reply("Controle da caixa de prêmios desconhecido.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String customId = event.getComponentId();
        if (!AccumulatorMessageFactory.isAccumulatorId(customId)) {
            return;
        }

        String[] parts = AccumulatorMessageFactory.parts(customId);
        if (!"color_select".equals(action(parts))) {
            return;
        }

        GuildContext context = context(event);
        if (context == null || event.getValues().isEmpty()) {
            event.reply("Seleção de cargo de cor inválida.").setEphemeral(true).queue();
            return;
        }

        ColorSelectAction selection = ColorSelectAction.parse(parts, event.getValues().getFirst());
        if (selection == null) {
            event.reply("Seleção de cargo de cor inválida.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> handleColorSelection(event, context.guild(), context.member(), selection));
    }

    private void handlePage(ButtonInteractionEvent event, Member member, String[] parts) {
        Integer page = intPart(parts, 3);
        if (page == null) {
            event.reply("Página inválida.").setEphemeral(true).queue();
            return;
        }

        if (!canUsePrizeList(member)) {
            event.reply("Você não pode usar esta lista de prêmios.").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        EXECUTOR.execute(() -> refreshMessage(event.getMessage(), event.getGuild(), page));
    }

    private void handleCurrency(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        CurrencyAction action = CurrencyAction.parse(parts);
        if (action == null) {
            event.reply("Ação de moeda inválida.").setEphemeral(true).queue();
            return;
        }

        if (requireConfigurablePrize(event, guild, member, action.prizeId(), UPDATE_PRIZE_TITLE, CONFIGURE_PERMISSION_MESSAGE) == null) {
            return;
        }

        event.deferEdit().queue();
        EXECUTOR.execute(() -> {
            prizeRepo.updateCurrency(guild.getIdLong(), action.prizeId(), action.currency(), Bot.unixNow());
            refreshMessage(event.getMessage(), guild, action.page());
        });
    }

    private void handleColorPrompt(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        PrizeAction action = PrizeAction.parse(parts);
        if (action == null) {
            event.reply("Ação de cargo de cor inválida.").setEphemeral(true).queue();
            return;
        }

        if (requireConfigurablePrize(event, guild, member, action.prizeId(), UPDATE_PRIZE_TITLE, CONFIGURE_PERMISSION_MESSAGE) == null) {
            return;
        }

        List<ColorRoleItem> colorRoles = availableColorRoles(guild);
        if (colorRoles.isEmpty()) {
            event.replyEmbeds(AccumulatorMessageFactory.failure(
                    "Nenhum cargo de cor",
                    "Nenhum cargo de cor configurado está disponível agora."
            )).setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(AccumulatorMessageFactory.setupSuccess("Escolha o cargo de cor para o prêmio `#" + action.prizeId() + "`."))
                .setEphemeral(true)
                .setComponents(ActionRow.of(AccumulatorMessageFactory.colorRoleMenu(
                        action.prizeId(),
                        action.page(),
                        event.getChannelIdLong(),
                        event.getMessageIdLong(),
                        guild,
                        colorRoles
                )))
                .queue();
    }

    private void handleReject(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        PrizeAction action = PrizeAction.parse(parts);
        if (action == null) {
            event.reply("Ação de rejeição inválida.").setEphemeral(true).queue();
            return;
        }

        if (requireConfigurablePrize(event, guild, member, action.prizeId(), REJECT_PRIZE_TITLE, REJECT_PERMISSION_MESSAGE) == null) {
            return;
        }

        event.deferEdit().queue();
        EXECUTOR.execute(() -> {
            prizeRepo.reject(guild.getIdLong(), action.prizeId(), member.getIdLong(), Bot.unixNow());
            refreshMessage(event.getMessage(), guild, action.page());
        });
    }

    private void handlePay(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        PrizeAction action = PrizeAction.parse(parts);
        if (action == null) {
            event.reply("Ação de pagamento inválida.").setEphemeral(true).queue();
            return;
        }

        if (!requireManageServer(event, member, "Só usuários com Gerenciar Servidor podem pagar prêmios acumulados.")) {
            return;
        }

        runApproval(event, guild, action.page(), () -> payoutService.approveOne(guild, action.prizeId(), member.getIdLong()));
    }

    private void handleApproveAll(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        Integer page = intPart(parts, 3);
        if (page == null) {
            event.reply("Ação de aprovar tudo inválida.").setEphemeral(true).queue();
            return;
        }

        if (!requireManageServer(event, member, "Só usuários com Gerenciar Servidor podem aprovar todos os prêmios acumulados.")) {
            return;
        }

        runApproval(event, guild, page, () -> payoutService.approveAll(guild, member.getIdLong()));
    }

    private void handleColorSelection(StringSelectInteractionEvent event, Guild guild, Member member, ColorSelectAction selection) {
        AccumulatorPrize prize = prizeRepo.findPendingById(guild.getIdLong(), selection.prizeId());
        if (prize == null) {
            editFailure(event.getHook(), UPDATE_PRIZE_TITLE, PRIZE_NOT_FOUND);
            return;
        }

        if (!canConfigure(member, prize)) {
            editFailure(event.getHook(), "Permissão insuficiente", CONFIGURE_PERMISSION_MESSAGE);
            return;
        }

        if (!isAvailableColorRole(guild, selection.roleId())) {
            editFailure(event.getHook(), "Cargo de cor inválido", "O cargo selecionado não está disponível como cargo de cor.");
            return;
        }

        boolean updated = prizeRepo.updateColorRole(guild.getIdLong(), selection.prizeId(), selection.roleId(), Bot.unixNow());
        if (!updated) {
            editFailure(event.getHook(), UPDATE_PRIZE_TITLE, PRIZE_NOT_FOUND);
            return;
        }

        refreshMessage(guild, selection.channelId(), selection.messageId(), selection.page());
        event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.setupSuccess(
                "Cargo de cor definido para o prêmio `#" + selection.prizeId() + "`."
        )).setComponents(List.of()).queue();
    }

    private AccumulatorPrize requireConfigurablePrize(
            ButtonInteractionEvent event,
            Guild guild,
            Member member,
            int prizeId,
            String notFoundTitle,
            String permissionMessage
    ) {
        AccumulatorPrize prize = prizeRepo.findPendingById(guild.getIdLong(), prizeId);
        if (prize == null) {
            replyFailure(event, notFoundTitle, PRIZE_NOT_FOUND);
            return null;
        }

        if (!canConfigure(member, prize)) {
            replyFailure(event, "Permissão insuficiente", permissionMessage);
            return null;
        }
        return prize;
    }

    private void runApproval(
            ButtonInteractionEvent event,
            Guild guild,
            int page,
            Supplier<AccumulatorApprovalReport> approval
    ) {
        event.deferReply(false).queue();
        EXECUTOR.execute(() -> {
            AccumulatorApprovalReport report = approval.get();
            refreshMessage(event.getMessage(), guild, page);

            MessageEmbed embedReport = AccumulatorMessageFactory.approvalReport(report, guild, event.getUser());
            event.getHook()
                    .editOriginalEmbeds(embedReport)
                    .queue();
        });
    }

    private boolean requireManageServer(ButtonInteractionEvent event, Member member, String description) {
        if (member.hasPermission(Permission.MANAGE_SERVER)) {
            return true;
        }

        replyFailure(event, "Permissão insuficiente", description);
        return false;
    }

    private boolean canUsePrizeList(Member member) {
        return member.hasPermission(Permission.BAN_MEMBERS) || member.hasPermission(Permission.MANAGE_SERVER);
    }

    private boolean canConfigure(Member member, AccumulatorPrize prize) {
        return member.hasPermission(Permission.MANAGE_SERVER) || member.getIdLong() == prize.getCreatedBy();
    }

    private List<ColorRoleItem> availableColorRoles(Guild guild) {
        return colorItemRepo.findAll().stream()
                .filter(item -> guild.getRoleById(item.getRoleId()) != null)
                .toList();
    }

    private boolean isAvailableColorRole(Guild guild, long roleId) {
        return colorItemRepo.findByRoleId(roleId) != null && guild.getRoleById(roleId) != null;
    }

    private void replyFailure(ButtonInteractionEvent event, String title, String description) {
        event.replyEmbeds(AccumulatorMessageFactory.failure(title, description))
                .setEphemeral(true)
                .queue();
    }

    private void editFailure(InteractionHook hook, String title, String description) {
        hook.editOriginalEmbeds(AccumulatorMessageFactory.failure(title, description))
                .setComponents(List.of())
                .queue();
    }

    private void refreshMessage(Message message, Guild guild, int pageIndex) {
        PageItem<AccumulatorPrize> page = page(guild.getIdLong(), pageIndex);
        message.editMessageComponents(AccumulatorMessageFactory.createList(guild, page))
                .useComponentsV2(true)
                .queue();
    }

    private void refreshMessage(Guild guild, long channelId, long messageId, int pageIndex) {
        GuildChannel channel = guild.getChannelCache().getElementById(channelId);
        if (!(channel instanceof MessageChannel messageChannel)) {
            return;
        }

        PageItem<AccumulatorPrize> page = page(guild.getIdLong(), pageIndex);
        messageChannel.editMessageComponentsById(messageId, AccumulatorMessageFactory.createList(guild, page))
                .useComponentsV2(true)
                .queue();
    }

    private PageItem<AccumulatorPrize> page(long guildId, int requestedPage) {
        Paginator<AccumulatorPrize> paginator = Paginator.of(
                offset -> prizeRepo.findPending(guildId, offset, AccumulatorMessageFactory.PAGE_SIZE),
                () -> prizeRepo.countPending(guildId),
                AccumulatorMessageFactory.PAGE_SIZE
        );

        PageItem<AccumulatorPrize> page = paginator.next(Math.max(0, requestedPage));
        if (page.isEmpty() && page.getRowCount() > 0) {
            return paginator.next(page.lastPageIndex());
        }
        return page;
    }

    private GuildContext context(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("Este botão só pode ser usado em um servidor.").setEphemeral(true).queue();
            return null;
        }
        return new GuildContext(guild, member);
    }

    private GuildContext context(StringSelectInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return null;
        }
        return new GuildContext(guild, member);
    }

    private static String action(String[] parts) {
        return parts.length > 2 ? parts[2] : "";
    }

    private static Integer intPart(String[] parts, int index) {
        return parts.length > index ? parseInteger(parts[index]) : null;
    }

    private static Long longPart(String[] parts, int index) {
        return parts.length > index ? parseLongValue(parts[index]) : null;
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLongValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record GuildContext(Guild guild, Member member) {}

    private record PrizeAction(int prizeId, int page) {
        private static PrizeAction parse(String[] parts) {
            Integer prizeId = intPart(parts, 3);
            Integer page = intPart(parts, 4);
            return prizeId == null || page == null ? null : new PrizeAction(prizeId, page);
        }
    }

    private record CurrencyAction(int prizeId, int page, CurrencyType currency) {
        private static CurrencyAction parse(String[] parts) {
            PrizeAction action = PrizeAction.parse(parts);
            CurrencyType currency = parts.length > 5 ? CurrencyType.fromName(parts[5]) : null;
            return action == null || currency == null
                    ? null
                    : new CurrencyAction(action.prizeId(), action.page(), currency);
        }
    }

    private record ColorSelectAction(int prizeId, int page, long channelId, long messageId, long roleId) {
        private static ColorSelectAction parse(String[] parts, String selectedValue) {
            Integer prizeId = intPart(parts, 3);
            Integer page = intPart(parts, 4);
            Long channelId = longPart(parts, 5);
            Long messageId = longPart(parts, 6);
            Long roleId = parseLongValue(selectedValue);
            if (prizeId == null || page == null || channelId == null || messageId == null || roleId == null) {
                return null;
            }
            return new ColorSelectAction(prizeId, page, channelId, messageId, roleId);
        }
    }
}
