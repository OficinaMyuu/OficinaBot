package ofc.bot.commands.impl.slash.groups;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
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

@DiscordCommand(name = "group pins")
public class GroupPinsCommand extends SlashSubcommand {
    private final OficinaGroupRepository grpRepo;

    public GroupPinsCommand(OficinaGroupRepository grpRepo) {
        this.grpRepo = grpRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        long userId = ctx.getUserId();
        boolean isPin = ctx.getSafeOption("action", OptionMapping::getAsString).equals("PIN");
        String msgId = ctx.getSafeOption("message-id", OptionMapping::getAsString);
        Member issuer = ctx.getIssuer();
        OficinaGroup group = grpRepo.findByOwnerId(userId);
        long msgIdLong;

        if (group == null)
            return Status.YOU_DO_NOT_OWN_A_GROUP;

        TextChannel chan = group.getTextChannel();
        if (chan == null)
            return Status.YOUR_GROUP_DOES_NOT_HAVE_TEXT_CHANNEL;

        try {
            msgIdLong = Long.parseLong(msgId);
        } catch (NumberFormatException e) {
            return Status.INVALID_ID_PROVIDED.args(msgId);
        }

        long chanId = chan.getIdLong();
        long guildId = ctx.getGuildId();
        boolean isFree = group.hasFreeAccess();
        int price = isFree ? 0 : StoreItemType.PIN_MESSAGE.getPrice();
        String msgUrl = String.format(Message.JUMP_URL, guildId, chanId, msgIdLong);
        Button confirm = getButton(isPin, group, msgIdLong, price);
        MessageEmbed embed = getEmbed(isPin, issuer, group, msgUrl, price);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(confirm)
                .send();
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Fixa/desfixa uma mensagem do chat do seu grupo.";
    }

    @NotNull
    @Override
    public Cooldown getCooldown() {
        return Cooldown.of(3, TimeUnit.SECONDS);
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.STRING, "action", "Se a mensagem deve ser fixada ou desfixada.", true)
                        .addChoice("📌 Pin", "PIN")
                        .addChoice("🗑 Unpin", "UNPIN"),

                new OptionData(OptionType.STRING, "message-id", "O ID da mensagem a ser fixada/desfixada.", true)
                        .setRequiredLength(18, 19)
        );
    }

    private Button getButton(boolean isPin, OficinaGroup group, long msgId, int price) {
        return isPin
                ? EntityContextFactory.createMessagePinConfirm(group, msgId, price)
                : EntityContextFactory.createMessageUnpinConfirm(group, msgId);
    }

    private MessageEmbed getEmbed(boolean isPin, Member issuer, OficinaGroup group, String msgUrl, int price) {
        return isPin
                ? EmbedFactory.embedGroupMessagePin(issuer, group, msgUrl, price)
                : EmbedFactory.embedGroupMessageUnpin(issuer, group, msgUrl);
    }
}