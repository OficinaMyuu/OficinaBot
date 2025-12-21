package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.Main;
import ofc.bot.domain.entity.Birthday;
import ofc.bot.domain.sqlite.repository.BirthdayRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;

@DiscordCommand(name = "davizinhobirthday")
public class DavizinhoBirthday extends SlashCommand {
    private final BirthdayRepository bdayRepo;

    public DavizinhoBirthday(BirthdayRepository bdayRepo) {
        this.bdayRepo = bdayRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        JDA api = Main.getApi();
        Long targetId = Bot.get("privilegedbirthdaycommand.davizinho.id", Long::parseLong);
        if (targetId == null) {
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }

        Birthday birthday = bdayRepo.findByUserId(targetId);
        if (birthday == null) {
            return Status.USER_IS_NOT_IN_BIRTHDAY_LIST;
        }

        api.retrieveUserById(targetId).queue((user) -> {
            MessageEmbed embed = EmbedFactory.embedPrivilegedBirthday(user, birthday);
            ctx.replyEmbeds(embed);
        }, (err) -> {
            ctx.reply(Status.USER_NOT_FOUND);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Mostra o aniversário do Davizinho.";
    }
}
