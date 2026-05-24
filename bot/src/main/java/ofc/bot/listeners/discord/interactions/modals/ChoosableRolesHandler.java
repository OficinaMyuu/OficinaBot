package ofc.bot.listeners.discord.interactions.modals;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.FileUpload;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.modals.contexts.ModalSubmitContext;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@InteractionHandler(scope = Scopes.Misc.CHOOSABLE_ROLES, autoResponseType = AutoResponseType.THINKING_EPHEMERAL)
public class ChoosableRolesHandler implements InteractionListener<ModalSubmitContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChoosableRolesHandler.class);

    @Override
    @SuppressWarnings("DataFlowIssue")
    public InteractionResult onExecute(ModalSubmitContext ctx) {
        Guild guild = ctx.getGuild();
        String title = ctx.getField("title");
        String desc = ctx.getField("desc", null); // It is preferred to just return null if the value is empty
        String opts = ctx.getField("opts");
        Message.Attachment img = ctx.find("image");
        long chanId = ctx.get("channel_id");
        int color = ctx.get("color");
        int maxChoices = ctx.get("max_choices");
        byte[] banner = downloadImage(img);
        TextChannel chan = guild.getTextChannelById(chanId);
        String filename = img == null ? null : img.getFileName();
        MessageEmbed embed = EmbedFactory.embedChoosableRoles(guild, title, desc, filename, color);
        List<FileUpload> files = banner.length == 0 ? List.of() : List.of(FileUpload.fromData(banner, filename));
        ResolvedOptions resolved = resolveOptions(guild, opts);

        if (chan == null) // ?
            return Status.CHANNEL_NOT_FOUND;

        if (!resolved.errors.isEmpty())
            return Status.ERRORS_ENCOUNTERED.args(resolved.prettyErrors());

        if (maxChoices > resolved.options.size())
            return Status.MAX_CHOICES_GREATER_THAN_TOTAL_OPTIONS.args(maxChoices, resolved.options.size());

        if (resolved.options.isEmpty())
            return Status.NO_OPTIONS_AT_SELECT_MENU;

        StringSelectMenu rolesMenu = StringSelectMenu.create("choosable_roles")
                .addOptions(resolved.options)
                .setMaxValues(maxChoices <= 0 ? resolved.options.size() : maxChoices)
                .setMinValues(0)
                .build();

        chan.sendMessageEmbeds(embed)
                .setFiles(files)
                .setComponents(ActionRow.of(rolesMenu))
                .queue();

        return ctx.reply(Status.DONE);
    }

    private byte[] downloadImage(Message.Attachment img) {
        if (img == null) return new byte[0];

        try {
            return img.getProxy().download().get().readAllBytes();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Could not download banner image", e);
        } catch (IOException e) {
            LOGGER.error("Could not read banner image", e);
        }
        return new byte[0];
    }

    private ResolvedOptions resolveOptions(Guild guild, String text) {
        String[] rows = text.split("\n");
        List<SelectOption> options = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String row : rows) {
            String[] args = row.strip().split(" ");
            if (args.length != 2) continue;

            Emoji emoji = Emoji.fromFormatted(args[0]);
            Role role = guild.getRoleById(args[1]);

            if (role == null) {
                errors.add("Invalid role: " + args[0]);
                continue;
            }

            SelectOption opt = SelectOption.of(role.getName(), role.getId())
                            .withEmoji(emoji);
            options.add(opt);
        }
        return new ResolvedOptions(errors, options);
    }

    private record ResolvedOptions(
            List<String> errors,
            List<SelectOption> options
    ) {
        ResolvedOptions(List<String> errors, List<SelectOption> options) {
            this.errors = errors;
            this.options = options.stream().limit(SelectMenu.OPTIONS_MAX_AMOUNT).toList();
        }
        String prettyErrors() {
            return errors.stream()
                    .map(e -> String.format("- %s.", e))
                    .collect(Collectors.joining("\n"));
        }
    }
}