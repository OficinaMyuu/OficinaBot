package ofc.bot.listeners.discord.guilds.messages;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import ofc.bot.domain.entity.AppUser;
import ofc.bot.domain.entity.WelcomedUser;
import ofc.bot.domain.sqlite.repository.UserRepository;
import ofc.bot.domain.sqlite.repository.WelcomedUserRepository;
import ofc.bot.handlers.interactions.EntityContextFactory;
import ofc.bot.util.Bot;
import ofc.bot.util.content.Staff;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import ofc.bot.util.embeds.EmbedFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DiscordEventHandler
public class WasWelcomedCommandHandler extends ListenerAdapter {
    private static final ErrorHandler MSG_ERROR_HANDLER = new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE);
    private final WelcomedUserRepository welcomedRepo;
    private final UserRepository userRepo;

    public WasWelcomedCommandHandler(WelcomedUserRepository welcomedRepo, UserRepository userRepo) {
        this.welcomedRepo = welcomedRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        Message msg = e.getMessage();
        String content = msg.getContentRaw();
        String[] args = content.split(" ");
        Member member = msg.getMember();

        if (!e.isFromGuild() || member == null || !args[0].equals(".wwd")) return;

        if (args.length < 2) {
            sendError(msg, "> ❌ Uso incorreto: `.wwd <user>`.");
            return;
        }

        long targetId = Bot.extractNumbers(args[1]);
        if (targetId == 0) {
            sendError(msg, "ID inválido.");
            return;
        }

        if (!Staff.isStaff(member)) return;

        Guild guild = member.getGuild();
        List<WelcomedUser> actions = welcomedRepo.findByTargetId(targetId);
        Button btn = EntityContextFactory.createStatelessWelcomeListButton();
        String targetName = resolveUsernameView(targetId);

        if (actions.isEmpty()) {
            MessageEmbed embed = EmbedFactory.embedEmptyWelcome(targetName);
            msg.replyEmbeds(embed)
                    .addComponents(ActionRow.of(btn))
                    .queue();
            return;
        }

        MessageEmbed embed = EmbedFactory.embedWelcomedUser(targetName, guild, actions);
        msg.replyEmbeds(embed)
                .addComponents(ActionRow.of(btn))
                .queue();
    }

    private String resolveUsernameView(long userId) {
        AppUser user = userRepo.findById(userId);
        return user == null ? Long.toString(userId) : user.getName();
    }

    private void sendError(Message msg, String content) {
        msg.reply(content).queue((r) -> scheduleDelete(r, msg));
    }

    private void scheduleDelete(Message... msgs) {
        for (Message msg : msgs) {
            msg.delete().queueAfter(10, TimeUnit.SECONDS, null, MSG_ERROR_HANDLER);
        }
    }

    @DiscordEventHandler
    public static class WasWelcomedCleanupHandler extends ListenerAdapter {

        @Override
        public void onButtonInteraction(ButtonInteractionEvent e) {
            String buttonId = e.getComponentId();
            Message msg = e.getMessage();

            if (!buttonId.equals("wwd_clear") || !e.isFromGuild()) return;

            GuildMessageChannel chan = msg.getGuildChannel();
            List<String> ids = resolveIds(msg);

            e.deferEdit().queue();
            chan.purgeMessagesById(ids);
        }

        private List<String> resolveIds(Message msg) {
            List<String> ids = new ArrayList<>(2);
            ids.add(msg.getId());

            MessageReference msgRef = msg.getMessageReference();
            if (msgRef != null) {
                ids.add(msgRef.getMessageId());
            }
            return ids;
        }
    }
}