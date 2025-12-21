package ofc.bot.commands.impl.slash.userinfo;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.User.Profile;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.*;
import ofc.bot.domain.entity.enums.Gender;
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.domain.viewmodels.MarriageView;
import ofc.bot.domain.viewmodels.UserinfoView;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.internal.data.BotProperties;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.List;

@DiscordCommand(name = "userinfo")
public class UserinfoCommand extends SlashCommand {
    private static final int MAX_MARRIAGE_DISPLAY = 20;
    private final CustomUserinfoRepository csInfoRepo;
    private final MemberEmojiRepository emjRepo;
    private final UserEconomyRepository ecoRepo;
    private final MarriageRepository marrRepo;
    private final OficinaGroupRepository groupRepo;

    public UserinfoCommand(
            CustomUserinfoRepository csInfoRepo, MemberEmojiRepository emjRepo,
            UserEconomyRepository ecoRepo, MarriageRepository marrRepo,
            OficinaGroupRepository groupRepo
    ) {
        this.csInfoRepo = csInfoRepo;
        this.emjRepo = emjRepo;
        this.ecoRepo = ecoRepo;
        this.marrRepo = marrRepo;
        this.groupRepo = groupRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member issuer = ctx.getIssuer();
        Member member = ctx.getOption("member", OptionMapping::getAsMember);
        Member target = member == null ? issuer : member;

        if (ctx.hasOption("member") && member == null)
            return Status.MEMBER_NOT_FOUND;

        target.getUser().retrieveProfile().queue((profile -> {
            long userId = target.getIdLong();
            UserinfoView userinfo = fetchUserinfo(userId);
            MessageEmbed embed = embed(userinfo, target, profile);

            ctx.replyEmbeds(embed);
        }));

        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Comando usado para saber informaçõe gerais do membro no servidor e do usuário do Discord.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O usuário a verificar as informações.")
        );
    }

    private MessageEmbed embed(UserinfoView cs, Member target, Profile profile) {
        EmbedBuilder builder = new EmbedBuilder();

        OffsetDateTime timeBoosted = target.getTimeBoosted();
        long boosterSince = timeBoosted == null ? 0 : timeBoosted.toEpochSecond();
        long creation = target.getUser().getTimeCreated().toEpochSecond();
        long joined = target.getTimeJoined().toEpochSecond();
        long groupRoleId = cs.group() == null ? 0 : cs.group().getRoleId();
        long balance = cs.balance();
        Guild guild = target.getGuild();
        Role groupRole = guild.getRoleById(groupRoleId);
        User user = target.getUser();
        Color color = getColor(cs, target);
        String title = getTitle(user);
        String description = getDescription(cs, target);
        String footer = getFooter(cs, guild);
        String banner = profile.getBannerUrl();
        String resizedBanner = banner == null ? null : banner + "?size=2048";

        builder
                .setTitle(title)
                .setDescription(description)
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setColor(color)
                .addField("📅 Criação da Conta", String.format("<t:%d>\n<t:%1$d:R>", creation), true)
                .addField("🌟 Entrou no Servidor", String.format("<t:%d>", joined), true)
                .addField(UserEconomy.SYMBOL + " Saldo", "$" + Bot.fmtNum(balance), true)
                .setFooter(footer, guild.getIconUrl());

        if (banner != null)
            builder.setImage(resizedBanner);

        if (groupRole != null)
            builder.addField("🎪 Grupo", groupRole.getAsMention(), true);

        if (boosterSince != 0)
            builder.addField("<:discordbooster:1094816233234378762> Booster Desde", "<t:" + boosterSince + ">", true);

        includeMarriagesIfPresent(target, cs, builder);
        return builder.build();
    }

    private void includeMarriagesIfPresent(Member member, UserinfoView cs, EmbedBuilder builder) {
        List<MarriageView> marriages = cs.marriages();

        if (marriages.isEmpty()) return;

        int relCount = cs.marriageCount();
        String formattedMarriages = formatMarriages(cs.userId(), marriages);
        String label = getMarriagesLabel(member, relCount);

        builder.addField(label, formattedMarriages, false);
    }

    private String getMarriagesLabel(Member member, int relCount) {
        if (relCount > 1) {
            String fmtCount = Bot.fmtNum(relCount);
            return String.format("💍 Casamentos (%s)", fmtCount);
        }

        Gender gender = Bot.findGender(member);
        return String.format("💍 Casad%s com", gender.getSuffix());
    }

    private String formatMarriages(long userId, List<MarriageView> marriages) {
        return Bot.format(marriages, (mr) -> {
            AppUser partner = mr.partner(userId);
            return String.format(CustomUserinfo.MARRIAGE_FORMAT, partner.getDisplayName(), mr.marriedAt());
        });
    }

    private UserinfoView fetchUserinfo(long userId) {
        CustomUserinfo csInfo = csInfoRepo.findByUserId(userId, CustomUserinfo.fromUserId(userId));
        UserEconomy userEco = ecoRepo.findByUserId(userId, UserEconomy.fromUserId(userId));
        OficinaGroup group = groupRepo.findByOwnerId(userId);
        List<MarriageView> rels = marrRepo.viewByUserId(userId, MAX_MARRIAGE_DISPLAY);
        int relCount = marrRepo.countByUserId(userId);

        return new UserinfoView(csInfo, group, rels, relCount, userId, userEco.getTotal());
    }

    private Color getColor(UserinfoView cs, Member member) {
        int color = cs.mods().getColorRaw();
        List<Role> roles = member.getRoles();

        if (color > 0) return new Color(color);

        return roles.isEmpty()
                ? Color.GRAY
                : roles.getFirst().getColor();
    }

    private String getTitle(User user) {
        long userId = user.getIdLong();
        boolean isDev = userId == BotProperties.DEV_ID;
        String name = user.getEffectiveName();
        String emojiView = resolveEmoji(userId, isDev);

        return emojiView == null
                ? String.format(CustomUserinfo.DEFAULT_TITLE_FORMAT, name)
                : emojiView + " " + name;
    }

    private String resolveEmoji(long userId, boolean isDev) {
        if (isDev) {
            String emojiFormat = BotProperties.fetch("dev.emoji");
            if (emojiFormat != null) return emojiFormat;
        }

        MemberEmoji emoji = emjRepo.findByUserId(userId);
        return emoji == null ? null : emoji.getEmoji();
    }

    private String getDescription(UserinfoView cs, Member member) {
        String desc = cs.mods().getDescription();

        return desc == null
                ? String.format(CustomUserinfo.DEFAULT_DESCRIPTION_FORMAT, member.getEffectiveName())
                : desc;
    }

    private String getFooter(UserinfoView cs, Guild guild) {
        String footer = cs.mods().getFooter();

        return footer == null
                ? guild.getName()
                : footer;
    }
}