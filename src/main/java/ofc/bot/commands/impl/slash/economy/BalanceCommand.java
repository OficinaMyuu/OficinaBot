package ofc.bot.commands.impl.slash.economy;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

@DiscordCommand(name = "balance")
public class BalanceCommand extends SlashCommand {
    private final UserEconomyRepository ecoRepo;

    public BalanceCommand(UserEconomyRepository econRepo) {
        this.ecoRepo = econRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User issuer = ctx.getUser();
        User target = ctx.getOption("user", issuer, OptionMapping::getAsUser);
        long userId = target.getIdLong();
        UserEconomy userEco = ecoRepo.findByUserId(userId, UserEconomy.fromUserId(userId));
        MessageEmbed embed = embed(target, userEco);

        return ctx.replyEmbeds(embed);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Verifica o saldo de um usuário.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a verificar o saldo.")
        );
    }

    private MessageEmbed embed(User user, UserEconomy eco) {
        EmbedBuilder builder = new EmbedBuilder();

        int rank = eco.isGenerated() ? 0 : ecoRepo.findRankByUser(eco);
        String fmtRank = rank == 0 ? "*Sem rank*" : "#" + rank;
        String name = user.getEffectiveName();
        String avatar = user.getEffectiveAvatarUrl();
        String fmtBank = Bot.fmtNum(eco.getBank());
        String fmtWallet = Bot.fmtNum(eco.getWallet());
        Color color = Bot.Colors.DEFAULT;

        return builder.setAuthor(name, null, avatar)
                .setDescription("Use `/leaderboard` para ver o ranking global.")
                .setColor(color)
                .addField(UserEconomy.WALLET_SYMBOL + " Cash", fmtWallet, true)
                .addField(UserEconomy.BANK_SYMBOL + " Bank", fmtBank, true)
                .addField(UserEconomy.RANK_SYMBOL + " Rank", fmtRank, true)
                .build();
    }
}