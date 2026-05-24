package ofc.bot.commands.impl.slash.relationships;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.MarriageRequest;
import ofc.bot.domain.sqlite.repository.*;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Roles;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@DiscordCommand(name = "marry")
public class MarryCommand extends SlashCommand {
    public static final int INITIAL_MARRIAGE_COST = 25000;
    public static final int DAILY_COST = 50;
    public static final int MAX_GENERAL_MARRIAGES = 10;
    public static final int MAX_PRIVILEGED_MARRIAGES = 20;
    private final MarriageRequestRepository mreqRepo;
    private final UserEconomyRepository ecoRepo;
    private final MarriageRepository marrRepo;
    private final UserRepository userRepo;

    public MarryCommand(MarriageRequestRepository mreqRepo, UserEconomyRepository ecoRepo, MarriageRepository marrRepo, UserRepository userRepo) {
        this.mreqRepo = mreqRepo;
        this.ecoRepo = ecoRepo;
        this.marrRepo = marrRepo;
        this.userRepo = userRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        Member sender = ctx.getIssuer();

        if (target == null)
            return Status.MEMBER_NOT_FOUND;

        MessageChannel channel = ctx.getChannel();
        long requesterId = sender.getIdLong();
        long targetId = target.getIdLong();
        boolean requesterHitLimit = hitLimit(sender);
        boolean targetHitLimit = hitLimit(target);
        boolean reqHasBalance = hasEnoughBalance(requesterId);
        boolean tarHasBalance = hasEnoughBalance(targetId);
        boolean privilegedMarriage = isPrivilegedMarriage(sender, target);

        if (requesterId == targetId || target.getUser().isBot())
            return Status.CANNOT_MARRY_TO_USER;

        if (mreqRepo.isPending(requesterId, targetId))
            return Status.PENDING_PROPOSAL;

        if (marrRepo.existsByUserIds(requesterId, targetId))
            return Status.ALREADY_MARRIED_TO_USER.args(target.getAsMention());

        if (requesterHitLimit)
            return Status.ISSUER_HIT_MARRIAGE_LIMIT;

        if (targetHitLimit)
            return Status.TARGET_HIT_MARRIAGE_LIMIT;

        if ((!reqHasBalance || !tarHasBalance) && !privilegedMarriage)
            return Status.MARRIAGE_INSUFFICIENT_BALANCE.args(Bot.fmtNum(INITIAL_MARRIAGE_COST));

        channel.sendMessageFormat("> ✨💍 %s você recebeu uma proposta de casamento maravilhosa de %s, safadeza hein?! 😏\nPara aceitar, use `/marriage accept`.",
                target.getAsMention(),
                sender.getAsMention()
        ).queue();

        MarriageRequest req = MarriageRequest.fromUsers(requesterId, targetId);
        mreqRepo.save(req);

        userRepo.upsert(AppUser.fromUser(sender.getUser()));
        userRepo.upsert(AppUser.fromUser(target.getUser()));

        return Status.MARRIAGE_PROPOSAL_SENT_SUCCESSFULLY.setEphm(true);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Se case com outra pessoa.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O usuário com quem você quer se casar.", true)
        );
    }

    private boolean hasEnoughBalance(long userId) {
        return ecoRepo.hasEnoughWallet(userId, MarryCommand.INITIAL_MARRIAGE_COST);
    }

    private boolean hitLimit(Member member) {
        if (member.hasPermission(Permission.MANAGE_SERVER))
            return false;

        boolean privileged = isPrivileged(member);
        int marriageCount = marrRepo.countByUserId(member.getIdLong());

        return privileged
                ? marriageCount >= MarryCommand.MAX_PRIVILEGED_MARRIAGES
                : marriageCount >= MarryCommand.MAX_GENERAL_MARRIAGES;
    }

    private boolean isPrivileged(Member member) {
        List<Role> roles = member.getRoles();
        Role salada = Roles.SALADA.role();

        return salada != null && roles.contains(salada);
    }

    private boolean isPrivilegedMarriage(Member spouse, Member anotherSpouse) {
        return isPermissionPrivileged(spouse) || isPermissionPrivileged(anotherSpouse);
    }

    private boolean isPermissionPrivileged(Member member) {
        return member != null && member.hasPermission(Permission.MANAGE_SERVER);
    }
}