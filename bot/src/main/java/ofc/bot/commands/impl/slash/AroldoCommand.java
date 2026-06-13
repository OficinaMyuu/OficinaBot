package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "aroldopediuessecomandoprovisorio", permissions = Permission.MANAGE_SERVER)
public class AroldoCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AroldoCommand.class);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        Member target = ctx.getOption("member", OptionMapping::getAsMember);
        String nickname = ctx.getSafeOption("nick", OptionMapping::getAsString);
        Guild guild = ctx.getGuild();
        Member self = guild.getSelfMember();

        if (!canRename(self, target)) {
            return ctx.reply("Nao consigo alterar o apelido desse membro pela hierarquia atual de cargos.", true);
        }

        ctx.ack(true);
        target.modifyNickname(nickname).queue(
                ignored -> ctx.edit("Apelido alterado com sucesso."),
                error -> {
                    LOGGER.error("Could not update nickname for member {} in guild {}", target.getId(), guild.getId(), error);
                    ctx.edit("Nao foi possivel alterar o apelido desse membro.");
                }
        );
        return Status.OK;
    }

    static boolean canRename(Member self, Member target) {
        return target != null && self.canInteract(target);
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Altera o apelido de um membro temporariamente.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "member", "O membro que tera o apelido alterado.", true),
                new OptionData(OptionType.STRING, "nick", "O novo apelido do membro.", true)
                        .setRequiredLength(1, 32)
        );
    }
}
