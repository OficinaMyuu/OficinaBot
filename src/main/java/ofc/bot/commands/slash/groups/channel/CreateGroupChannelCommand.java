package ofc.bot.commands.slash.groups.channel;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.domain.sqlite.repository.OficinaGroupRepository;
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

@DiscordCommand(name = "group channel create")
public class CreateGroupChannelCommand extends SlashSubcommand {
    private final OficinaGroupRepository grpRepo;

    public CreateGroupChannelCommand(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        OficinaGroup group = grpRepo.findByOwnerId(userId);
        Member issuer = ctx.getIssuer();
        ChannelType chanType = ctx.getSafeEnumOption("type", ChannelType.class);
        Guild guild = ctx.getGuild();

        if (group == null)
            return Status.YOU_DO_NOT_OWN_A_GROUP;

        if (hitMaxChannels(guild))
            return Status.GROUP_CHANNELS_CANNOT_BE_CREATED;

        // Err... is this even possible?
        if (chanType != ChannelType.TEXT && chanType != ChannelType.VOICE)
            return Status.INVALID_CHANNEL_TYPE.args(chanType);

        if (hasChannelOfType(group, chanType))
            return Status.GROUP_ALREADY_HAS_THE_PROVIDED_CHANNEL;

        StoreItemType itemType = resolveStoreItem(chanType);
        int price = group.hasFreeAccess() ? 0 : itemType.getPrice();

        Button confirm = EntityContextFactory.createGroupChannelConfirm(group, chanType, price);
        MessageEmbed embed = EmbedFactory.embedGroupChannelCreate(issuer, group, chanType, price);
        return ctx.create()
                .setActionRows(confirm)
                .setEmbeds(embed)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Cria um novo canal ao grupo.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(10, TimeUnit.SECONDS);
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "type", "O tipo de canal a ser criado", true)
                        .addChoice("🔊 Voice", ChannelType.VOICE.name())
                        .addChoice("📖 Text", ChannelType.TEXT.name())
        );
    }

    private boolean hasChannelOfType(OficinaGroup group, ChannelType type) {
        return type == ChannelType.TEXT
                ? group.hasTextChannel()
                : group.hasVoiceChannel();
    }

    private boolean hitMaxChannels(Guild guild) {
        int chanSize = guild.getChannels().size();
        // Yes, this is a magic value.
        // Since JDA does not provide a constant value for this, I am keeping it as a magic value.
        return chanSize >= 500;
    }

    private StoreItemType resolveStoreItem(ChannelType type) {
        return type == ChannelType.TEXT
                ? StoreItemType.GROUP_TEXT_CHANNEL
                : StoreItemType.GROUP_VOICE_CHANNEL;
    }
}