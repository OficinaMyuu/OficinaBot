package ofc.bot.listeners.discord.interactions.buttons.accumulator;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.entity.AccumulatorPrize;
import ofc.bot.domain.entity.ColorRoleItem;
import ofc.bot.domain.sqlite.repository.AccumulatorPrizeRepository;
import ofc.bot.domain.sqlite.repository.ColorRoleItemRepository;
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

@DiscordEventHandler
public class AccumulatorInteractionListener extends ListenerAdapter {
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
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

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("Este botão só pode ser usado em um servidor.").setEphemeral(true).queue();
            return;
        }

        String[] parts = AccumulatorMessageFactory.parts(customId);
        if (parts.length < 4) {
            event.reply("Controle da caixa de prêmios inválido.").setEphemeral(true).queue();
            return;
        }

        String action = parts[2];
        switch (action) {
            case "page" -> handlePage(event, member, parts);
            case "currency" -> handleCurrency(event, guild, member, parts);
            case "color" -> handleColorPrompt(event, guild, member, parts);
            case "reject" -> handleReject(event, guild, member, parts);
            case "pay" -> handlePay(event, guild, member, parts);
            case "all" -> handleApproveAll(event, guild, member, parts);
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
        if (parts.length < 7 || !"color_select".equals(parts[2])) {
            return;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null || event.getValues().isEmpty()) {
            event.reply("Seleção de cargo de cor inválida.").setEphemeral(true).queue();
            return;
        }

        Integer prizeId = parseInt(parts[3]);
        Integer page = parseInt(parts[4]);
        Long channelId = parseLong(parts[5]);
        Long messageId = parseLong(parts[6]);
        Long roleId = parseLong(event.getValues().getFirst());
        if (prizeId == null || page == null || channelId == null || messageId == null || roleId == null) {
            event.reply("Seleção de cargo de cor inválida.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            AccumulatorPrize prize = prizeRepo.findPendingById(guild.getIdLong(), prizeId);
            if (prize == null) {
                event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.failure(
                        "Não foi possível atualizar o prêmio",
                        "Prêmio não encontrado ou já processado."
                )).queue();
                return;
            }

            if (!canConfigure(member, prize)) {
                event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.failure(
                        "Permissão insuficiente",
                        "Só quem adicionou este prêmio ou usuários com Gerenciar Servidor podem alterá-lo."
                )).queue();
                return;
            }

            if (colorItemRepo.findByRoleId(roleId) == null || guild.getRoleById(roleId) == null) {
                event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.failure(
                        "Cargo de cor inválido",
                        "O cargo selecionado não está disponível como cargo de cor."
                )).queue();
                return;
            }

            boolean updated = prizeRepo.updateColorRole(guild.getIdLong(), prizeId, roleId, Bot.unixNow());
            if (!updated) {
                event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.failure(
                        "Não foi possível atualizar o prêmio",
                        "Prêmio não encontrado ou já processado."
                )).queue();
                return;
            }

            refreshMessage(guild, channelId, messageId, page);
            event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.setupSuccess(
                    "Cargo de cor definido para o prêmio `#" + prizeId + "`."
            )).setComponents(List.of()).queue();
        });
    }

    private void handlePage(ButtonInteractionEvent event, Member member, String[] parts) {
        if (!member.hasPermission(Permission.BAN_MEMBERS) && !member.hasPermission(Permission.MANAGE_SERVER)) {
            event.reply("Você não pode usar esta lista de prêmios.").setEphemeral(true).queue();
            return;
        }

        Integer page = parseInt(parts[3]);
        if (page == null) {
            event.reply("Página inválida.").setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        EXECUTOR.execute(() -> refreshMessage(event.getMessage(), event.getGuild(), page));
    }

    private void handleCurrency(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        Integer prizeId = parts.length > 3 ? parseInt(parts[3]) : null;
        Integer page = parts.length > 4 ? parseInt(parts[4]) : null;
        CurrencyType currency = parts.length > 5 ? CurrencyType.fromName(parts[5]) : null;
        if (prizeId == null || page == null || currency == null) {
            event.reply("Ação de moeda inválida.").setEphemeral(true).queue();
            return;
        }

        AccumulatorPrize prize = prizeRepo.findPendingById(guild.getIdLong(), prizeId);
        if (prize == null) {
            event.replyEmbeds(AccumulatorMessageFactory.failure("Não foi possível atualizar o prêmio", "Prêmio não encontrado ou já processado."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!canConfigure(member, prize)) {
            event.replyEmbeds(AccumulatorMessageFactory.failure(
                    "Permissão insuficiente",
                    "Só quem adicionou este prêmio ou usuários com Gerenciar Servidor podem alterá-lo."
            )).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            boolean updated = prizeRepo.updateCurrency(guild.getIdLong(), prizeId, currency, Bot.unixNow());
            refreshMessage(event.getMessage(), guild, page);
            event.getHook().editOriginalEmbeds(updated
                    ? AccumulatorMessageFactory.setupSuccess("Moeda definida para o prêmio `#" + prizeId + "`.")
                    : AccumulatorMessageFactory.failure("Não foi possível atualizar o prêmio", "Prêmio não encontrado ou já processado.")
            ).queue();
        });
    }

    private void handleColorPrompt(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        Integer prizeId = parts.length > 3 ? parseInt(parts[3]) : null;
        Integer page = parts.length > 4 ? parseInt(parts[4]) : null;
        if (prizeId == null || page == null) {
            event.reply("Ação de cargo de cor inválida.").setEphemeral(true).queue();
            return;
        }

        AccumulatorPrize prize = prizeRepo.findPendingById(guild.getIdLong(), prizeId);
        if (prize == null) {
            event.replyEmbeds(AccumulatorMessageFactory.failure("Não foi possível atualizar o prêmio", "Prêmio não encontrado ou já processado."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!canConfigure(member, prize)) {
            event.replyEmbeds(AccumulatorMessageFactory.failure(
                    "Permissão insuficiente",
                    "Só quem adicionou este prêmio ou usuários com Gerenciar Servidor podem alterá-lo."
            )).setEphemeral(true).queue();
            return;
        }

        List<ColorRoleItem> colorRoles = colorItemRepo.findAll().stream()
                .filter(item -> guild.getRoleById(item.getRoleId()) != null)
                .toList();
        if (colorRoles.isEmpty()) {
            event.replyEmbeds(AccumulatorMessageFactory.failure(
                    "Nenhum cargo de cor",
                    "Nenhum cargo de cor configurado está disponível agora."
            )).setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(AccumulatorMessageFactory.setupSuccess("Escolha o cargo de cor para o prêmio `#" + prizeId + "`."))
                .setEphemeral(true)
                .setComponents(ActionRow.of(AccumulatorMessageFactory.colorRoleMenu(
                        prizeId,
                        page,
                        event.getChannelIdLong(),
                        event.getMessageIdLong(),
                        guild,
                        colorRoles
                )))
                .queue();
    }

    private void handleReject(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        Integer prizeId = parts.length > 3 ? parseInt(parts[3]) : null;
        Integer page = parts.length > 4 ? parseInt(parts[4]) : null;
        if (prizeId == null || page == null) {
            event.reply("Ação de rejeição inválida.").setEphemeral(true).queue();
            return;
        }

        AccumulatorPrize prize = prizeRepo.findPendingById(guild.getIdLong(), prizeId);
        if (prize == null) {
            event.replyEmbeds(AccumulatorMessageFactory.failure("Não foi possível rejeitar o prêmio", "Prêmio não encontrado ou já processado."))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!canConfigure(member, prize)) {
            event.replyEmbeds(AccumulatorMessageFactory.failure(
                    "Permissão insuficiente",
                    "Só quem adicionou este prêmio ou usuários com Gerenciar Servidor podem rejeitá-lo."
            )).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            boolean updated = prizeRepo.reject(guild.getIdLong(), prizeId, member.getIdLong(), Bot.unixNow());
            refreshMessage(event.getMessage(), guild, page);
            event.getHook().editOriginalEmbeds(updated
                    ? AccumulatorMessageFactory.setupSuccess("Prêmio `#" + prizeId + "` rejeitado.")
                    : AccumulatorMessageFactory.failure("Não foi possível rejeitar o prêmio", "Prêmio não encontrado ou já processado.")
            ).queue();
        });
    }

    private void handlePay(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        Integer prizeId = parts.length > 3 ? parseInt(parts[3]) : null;
        Integer page = parts.length > 4 ? parseInt(parts[4]) : null;
        if (prizeId == null || page == null) {
            event.reply("Ação de pagamento inválida.").setEphemeral(true).queue();
            return;
        }

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            event.replyEmbeds(AccumulatorMessageFactory.failure(
                    "Permissão insuficiente",
                    "Só usuários com Gerenciar Servidor podem pagar prêmios acumulados."
            )).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            AccumulatorApprovalReport report = payoutService.approveOne(guild, prizeId, member.getIdLong());
            refreshMessage(event.getMessage(), guild, page);
            event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.approvalReport(report, event.getUser())).queue();
        });
    }

    private void handleApproveAll(ButtonInteractionEvent event, Guild guild, Member member, String[] parts) {
        Integer page = parseInt(parts[3]);
        if (page == null) {
            event.reply("Ação de aprovar tudo inválida.").setEphemeral(true).queue();
            return;
        }

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            event.replyEmbeds(AccumulatorMessageFactory.failure(
                    "Permissão insuficiente",
                    "Só usuários com Gerenciar Servidor podem aprovar todos os prêmios acumulados."
            )).setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        EXECUTOR.execute(() -> {
            AccumulatorApprovalReport report = payoutService.approveAll(guild, member.getIdLong());
            refreshMessage(event.getMessage(), guild, page);
            event.getHook().editOriginalEmbeds(AccumulatorMessageFactory.approvalReport(report, event.getUser())).queue();
        });
    }

    private boolean canConfigure(Member member, AccumulatorPrize prize) {
        return member.hasPermission(Permission.MANAGE_SERVER) || member.getIdLong() == prize.getCreatedBy();
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

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
