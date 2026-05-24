package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashCommand;
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordCommand(name = "rolemembers")
public class RoleMembersCommand extends SlashCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleMembersCommand.class);

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        ctx.ack();

        Role role = ctx.getSafeOption("role", OptionMapping::getAsRole);
        Guild guild = ctx.getGuild();

        guild.findMembersWithRoles(role).setTimeout(30, TimeUnit.SECONDS).onSuccess((members) -> {
            int count = members.size();

            if (count == 0) {
                ctx.reply(Status.ROLE_HAS_NO_MEMBERS);
                return;
            }

            String statistics = getPrettyStatistics(role, members);
            String formatMembers = format(members);

            handleResponse(formatMembers, statistics, ctx);
        }).onError((e) -> {
            LOGGER.error("Could not perform role members lookup", e);
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });
        return Status.OK;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Veja os membros de um cargo.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.ROLE, "role", "O cargo a verificar os membros.", true)
        );
    }

    private void handleResponse(String formattedMembers, String statistics, SlashCommandContext cmd) {
        String output = statistics + "```txt\n" + formattedMembers + "```";

        if (output.length() > Message.MAX_CONTENT_LENGTH) {
            handleAsFile(formattedMembers, statistics, cmd);
            return;
        }
        cmd.reply(output);
    }

    private void handleAsFile(String formattedMembers, String statistics, SlashCommandContext cmd) {
        cmd.create()
                .setContent(statistics)
                .setFiles(FileUpload.fromData(formattedMembers.getBytes(), "members.txt"))
                .send();
    }

    private String getPrettyStatistics(Role role, List<Member> members) {
        List<Member> online = members.stream().filter((m) -> m.getOnlineStatus() != OnlineStatus.OFFLINE).toList();
        List<Member> offline = members.stream().filter((m) -> m.getOnlineStatus() == OnlineStatus.OFFLINE).toList();

        return String.format("""
                Cargo `%s`
                Online: `%s`
                Offline: `%s`
                Total: `%s`
                """,
                role.getName(),
                Bot.fmtNum(online.size()),
                Bot.fmtNum(offline.size()),
                Bot.fmtNum(members.size())
        );
    }

    protected static String format(final List<Member> members) {
        int maxLength = 0;
        List<String> ids = members.stream().map(Member::getId).toList();
        StringBuilder formatted = new StringBuilder();

        for (String id : ids) {
            int length = id.length();

            if (length > maxLength)
                maxLength = length;
        }

        for (Member m : members) {
            String name = m.getUser().getName();
            String id = m.getId();
            int spaces = maxLength - id.length();

            String line = id + " ".repeat(Math.max(0, spaces)) +
                    "  ->  " + name;

            formatted.append(line);
            formatted.append("\n");
        }
        return formatted.toString().strip();
    }
}