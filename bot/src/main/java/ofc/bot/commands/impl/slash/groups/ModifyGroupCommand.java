package ofc.bot.commands.impl.slash.groups;

import com.vdurmont.emoji.EmojiManager;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.database.repository.OficinaGroupRepository;
import ofc.bot.domain.database.repository.StoreItemSettingsRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.handlers.interactions.commands.Cooldown;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "group modify")
public class ModifyGroupCommand extends SlashSubcommand {
    private final OficinaGroupRepository grpRepo;
    private final StoreItemSettingsRepository storeItemSettingsRepo;

    public ModifyGroupCommand(
            OficinaGroupRepository grpRepo,
            StoreItemSettingsRepository storeItemSettingsRepo
    ) {
        this.grpRepo = grpRepo;
        this.storeItemSettingsRepo = storeItemSettingsRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member issuer = ctx.getIssuer();
        String newName = ctx.getOption("new-name", OptionMapping::getAsString);
        String newColorHex = ctx.getOption("new-color", OptionMapping::getAsString);
        String newEmoji = ctx.getOption("new-emoji", OptionMapping::getAsString);
        long userId = ctx.getUserId();
        boolean isEmpty = !ctx.hasOptions();

        if (isEmpty)
            return Status.NOTHING_CHANGED_WITH_REASON.args("nenhum argumento foi fornecido");

        OficinaGroup group = grpRepo.findByOwnerId(userId);

        if (group == null)
            return Status.YOU_DO_NOT_OWN_A_GROUP;

        int price = 0;
        int newColor = -1;

        if (newName != null && EmojiManager.containsEmoji(newName))
            return Status.GROUP_NAMES_CANNOT_CONTAIN_EMOJIS;

        if (newEmoji != null && !EmojiManager.isEmoji(newEmoji))
            return Status.EMOJI_OPTION_CAN_ONLY_CONTAIN_EMOJI;

        if (newEmoji != null && grpRepo.existsByEmojiExceptId(newEmoji, group.getId()))
            return Status.GROUP_EMOJI_ALREADY_IN_USE;

        if (newColorHex != null) {
            try {
                newColor = Integer.parseInt(newColorHex, 16);
            } catch (NumberFormatException e) {
                return Status.INVALID_COLOR_PROVIDED;
            }
        }

        if (!OficinaGroup.hasFreeAccess(issuer)) {
            var configuredPrice = storeItemSettingsRepo.findPrice(StoreItemType.UPDATE_GROUP);
            if (configuredPrice.isEmpty())
                return Status.STORE_ITEM_CONFIGURATION_UNAVAILABLE;

            int updatedAttributes = 0;
            if (newName != null) updatedAttributes++;
            if (newColorHex != null) updatedAttributes++;
            if (newEmoji != null) updatedAttributes++;

            try {
                price = Math.multiplyExact(configuredPrice.getAsInt(), updatedAttributes);
            } catch (ArithmeticException ignored) {
                return Status.STORE_ITEM_CONFIGURATION_UNAVAILABLE;
            }
        }

        Button confirmButton = EntityContextFactory.createModifyGroupConfirm(group, newName, newEmoji, newColor, price);
        MessageEmbed embed = EmbedFactory.embedGroupModify(issuer, group, newName, newEmoji, newColor, price);
        return ctx.create()
                .setActionRows(confirmButton)
                .setEmbeds(embed)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Modifica dados do seu grupo, como nome e cor.";
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
                new OptionData(OptionType.STRING, "new-name", "O novo nome do grupo.")
                        .setRequiredLength(OficinaGroup.MIN_NAME_LENGTH, OficinaGroup.MAX_NAME_LENGTH),

                new OptionData(OptionType.STRING, "new-color", "A nova cor do grupo.")
                        .setRequiredLength(6, 6),

                new OptionData(OptionType.STRING, "new-emoji", "O novo emoji utilizado para criar chats e calls.")
                        .setRequiredLength(1, 50)
        );
    }
}
