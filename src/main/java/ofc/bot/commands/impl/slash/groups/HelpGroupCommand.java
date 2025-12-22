package ofc.bot.commands.impl.slash.groups;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.domain.entity.OficinaGroup;
import ofc.bot.domain.entity.enums.StoreItemType;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

@DiscordCommand(name = "group help")
public class HelpGroupCommand extends SlashSubcommand {

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Guild guild = ctx.getGuild();
        MessageEmbed embed = embed(guild);

        return ctx.replyEmbeds(true, embed);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra ajuda/informações sobre os grupos.";
    }

    private MessageEmbed embed(Guild guild) {
        EmbedBuilder builder = new EmbedBuilder();

        return builder
                .setTitle("Grupos 🔐")
                .setColor(Bot.Colors.DEFAULT)
                .setDescription(getHelpText())
                .setFooter(guild.getName(), guild.getIconUrl())
                .build();
    }

    private String getHelpText() {
        int refundPercent = (int) (OficinaGroup.REFUND_PERCENT * 100); // Convert to decimal
        String price = Bot.fmtNum(StoreItemType.GROUP.getPrice());
        String textPrice = Bot.fmtNum(StoreItemType.GROUP_TEXT_CHANNEL.getPrice());
        String voicePrice = Bot.fmtNum(StoreItemType.GROUP_VOICE_CHANNEL.getPrice());
        String botPrice = Bot.fmtNum(StoreItemType.ADDITIONAL_BOT.getPrice());

        return String.format("""
                Grupos permitem criar canais de texto e voz privados para você e seus amigos, \
                mas seguem as regras gerais do servidor e possuem custos mensais. \
                Abaixo estão as principais informações:

                # Aluguel 🏡
                - **Primeiro mês grátis.**
                - Cobrança mensal todo __dia 1__ diretamente do dono grupo.
                - Valor: `$1.000` *por membro* (exceto membros do cargo <@&592427146912464919> pra cima).
                - Em caso de saldo insuficiente:
                  1. Um aviso é enviado ao dono com o valor pendente.
                  2. Nova tentativa é feita após 24 horas.
                  3. Se ainda insuficiente, o grupo poderá ser suspenso.

                # Reembolso 💰
                - O reembolso é de `%d%%` para os seguintes itens:
                  - O __valor pago__ no grupo.
                  - O __valor pago__ no canal de texto.
                  - O __valor pago__ no canal de voz.
                - Exemplo: Se você pagou `$300.000` por um canal e o preço atual for `$700.000`, \
                o reembolso será baseado no valor que você pagou (`$300.000`).

                # Preços 💸
                - **Grupo:** `$%s`.
                - **Canal de Texto:** `$%s`.
                - **Canal de Voz:** `$%s`.
                - **Bot Adicional:** `$%s` cada (`/group bots` em breve).

                Use `/group info` para informações específicas sobre seu grupo.
                """, refundPercent, price, textPrice, voicePrice, botPrice);
    }
}