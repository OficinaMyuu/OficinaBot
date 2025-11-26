package ofc.bot.commands.slash.bets;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.games.betting.BetManager;
import ofc.bot.handlers.games.betting.tictactoe.TicTacToeGame;
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

@DiscordCommand(name = "bets tictactoe")
public class BetTicTacToeCommand extends SlashSubcommand {
    private static final BetManager betManager = BetManager.getManager();
    private final UserEconomyRepository ecoRepo;

    public BetTicTacToeCommand(UserEconomyRepository ecoRepo) {
        this.ecoRepo = ecoRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        int amount = ctx.getSafeOption("amount", OptionMapping::getAsInt);
        Member member = ctx.getOption("member", OptionMapping::getAsMember);
        User issuer = ctx.getUser();
        Guild guild = ctx.getGuild();
        long issuerId = issuer.getIdLong();

        if (member == null)
            return Status.MEMBER_NOT_FOUND;

        long memberId = member.getIdLong();
        if (member.getUser().isBot() || memberId == issuerId)
            return Status.YOU_CANNOT_BET_THIS_USER;

        InteractionResult err = checks(issuerId, memberId, amount);
        if (err != null) return err;

        MessageEmbed embed = EmbedFactory.embedTicTacToeCreate(guild, issuer, member.getUser(), amount);
        Button accept = EntityContextFactory.createTicTacToeInvite(issuerId, memberId, amount);

        return ctx.create()
                .setEmbeds(embed)
                .setActionRows(accept)
                .setContent(Status.USER_INVITING_TO_TICTACTOE.args(member.getAsMention()))
                .send();
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Compita Jogo da velha contra outro usuário.";
    }

    @Override
    @NotNull
    public Cooldown getCooldown() {
        return Cooldown.of(2, TimeUnit.MINUTES);
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro a competir contra.", true),
                new OptionData(OptionType.INTEGER, "amount", "A quantia a ser apostada.", true)
                        .setRequiredRange(TicTacToeGame.MIN_AMOUNT, TicTacToeGame.MAX_AMOUNT)
        );
    }

    private InteractionResult checks(long issuerId, long otherId, int amount) {
        if (betManager.isBetting(issuerId)) return Status.YOU_CANNOT_DO_THIS_WHILE_BETTING;
        if (betManager.isBetting(otherId)) return Status.MEMBER_IS_BETTING_PLEASE_WAIT.args(otherId);

        if (!ecoRepo.hasEnoughBank(issuerId, amount)) return Status.INSUFFICIENT_BALANCE;
        if (!ecoRepo.hasEnoughBank(otherId, amount)) return Status.MEMBER_INSUFFICIENT_BALANCE.args(otherId);
        return null;
    }
}