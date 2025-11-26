package ofc.bot.commands.slash.groups;

import com.vdurmont.emoji.EmojiManager;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.RentStatus;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Group creation after user confirmation is handled at
 * {@link ofc.bot.listeners.discord.interactions.buttons.groups.GroupCreationHandler GroupCreationHandler}.
 */
@DiscordCommand(name = "group create")
public class CreateGroupCommand extends SlashSubcommand {
    private final OficinaGroupRepository grpRepo;

    public CreateGroupCommand(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        String name = ctx.getSafeOption("name", OptionMapping::getAsString).strip();
        String hexColor = ctx.getSafeOption("color", OptionMapping::getAsString);
        String emoji = ctx.getSafeOption("emoji", OptionMapping::getAsString);
        CurrencyType currency = ctx.getEnumOption("currency", CurrencyType.UNBELIEVABOAT, CurrencyType.class);
        Member issuer = ctx.getIssuer();
        Guild guild = ctx.getGuild();
        long issuerId = issuer.getIdLong();
        long guildId = guild.getIdLong();
        int color;

        if (hasGroup(issuerId))
            return Status.USERS_CANNOT_HAVE_MULTIPLE_GROUPS;

        if (hitMaxRoles(guild))
            return Status.GROUPS_CANNOT_BE_CREATED_AT_THE_MOMENT;

        if (EmojiManager.containsEmoji(name))
            return Status.GROUP_NAMES_CANNOT_CONTAIN_EMOJIS;

        if (!EmojiManager.isEmoji(emoji))
            return Status.EMOJI_OPTION_CAN_ONLY_CONTAIN_EMOJI;

        try {
            color = Integer.parseInt(hexColor, 16);
        } catch (NumberFormatException e) {
            return Status.INVALID_COLOR_PROVIDED;
        }

        boolean isRentRecurring = OficinaGroup.isRentRecurring(issuer);
        boolean isFree = OficinaGroup.hasFreeAccess(issuer);
        int price = isFree ? 0 : StoreItemType.GROUP.getPrice();
        float refundPercent = isFree ? 0 : OficinaGroup.REFUND_PERCENT;
        RentStatus rentStatus = isRentRecurring ? RentStatus.TRIAL : RentStatus.FREE;
        // If all checks passed, we create sort of a partial group instance,
        // containing all the information we have so far.
        // We are not sending the group to the database as we must wait
        // for the user to confirm the purchase (handled at ofc.bot.listeners.buttons.groups.GroupCreationHandler).
        OficinaGroup group = new OficinaGroup(name, issuerId, guildId, rentStatus, isFree)
                .setEmoji(emoji)
                .setInvoiceAmount(0) // No invoices to pay initially
                .setCurrency(currency)
                .setAmountPaid(price)
                .setRefundPercent(refundPercent);

        Button confirmButton = EntityContextFactory.createGroupConfirm(group, color);
        MessageEmbed embed = EmbedFactory.embedGroupCreate(issuer, group, color);
        return ctx.create()
                .setActionRows(confirmButton)
                .setEmbeds(embed)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cria um grupo novo em seu nome.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(1, TimeUnit.MINUTES);
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "name", "O nome do grupo. Você não pode usar emojis.", true)
                        .setRequiredLength(OficinaGroup.MIN_NAME_LENGTH, OficinaGroup.MAX_NAME_LENGTH),

                new OptionData(OptionType.STRING, "color", "A cor do grupo em formato HEX (sem o #).", true)
                        .setRequiredLength(6, 6),

                new OptionData(OptionType.STRING, "currency", "Qual o tipo de economia deve ser usada para efetuar cobranças nesse grupo.", true)
                        .addChoices(getCurrencyChoices()),

                new OptionData(OptionType.STRING, "emoji", "O emoji utilizado para criar chats e calls (forneça o emoji em si, ex: 💪).", true)
                        .setRequiredLength(1, 50) // Ahh, yeah, you are probably wondering why max to 50 chars... I also don't know
        );
    }

    private boolean hasGroup(long userId) {
        return grpRepo.existsByOwnerId(userId);
    }

    private boolean hitMaxRoles(Guild guild) {
        List<Role> roles = guild.getRoles();
        // Yes, this is a magic value.
        // Since JDA does not provide a constant value for this, I am keeping it as a magic value.
        return roles.size() >= 250;
    }

    private List<Command.Choice> getCurrencyChoices() {
        return Arrays.stream(CurrencyType.values())
                .map((ct) -> new Command.Choice(ct.getName(), ct.toString()))
                .toList();
    }
}
