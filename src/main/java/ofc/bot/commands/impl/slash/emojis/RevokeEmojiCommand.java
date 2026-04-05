package ofc.bot.commands.impl.slash.emojis;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.domain.entity.MemberEmoji;
import ofc.bot.domain.entity.UserEmojiPermission;
import ofc.bot.domain.sqlite.repository.MemberEmojiRepository;
import ofc.bot.domain.sqlite.repository.UserEmojiPermissionRepository;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "emojis revoke")
public class RevokeEmojiCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevokeEmojiCommand.class);
    private final MemberEmojiRepository emojiRepo;
    private final UserEmojiPermissionRepository emojiPermRepo;

    public RevokeEmojiCommand(MemberEmojiRepository emojiRepo, UserEmojiPermissionRepository emojiPermRepo) {
        this.emojiRepo = emojiRepo;
        this.emojiPermRepo = emojiPermRepo;
    }

    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        User actor = ctx.getUser();
        User target = ctx.getSafeOption("user", OptionMapping::getAsUser);
        long actorId = actor.getIdLong();
        long targetId = target.getIdLong();
        MemberEmoji emoji = emojiRepo.findByUserId(actorId);

        if (emoji == null)
            return Status.YOU_DO_NOT_OWN_AN_EMOJI.args(target.getAsMention());

        UserEmojiPermission perm = emojiPermRepo.findByUserAndEmoji(targetId, emoji.getEmoji());
        if (perm == null)
            return Status.EMOJI_NOT_AUTHORIZED_TO_USER.args(target.getAsMention());

        try {
            emojiPermRepo.delete(perm);
            return Status.EMOJI_PERMISSION_SUCCESSFULLY_REVOKED.args(emoji.getEmoji(), target.getAsMention());
        } catch (DataAccessException e) {
            LOGGER.error("Failed to revoke emoji permission to user {}", targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Remove a autorização de emoji de um membro.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a ter a autorização removida.", true)
        );
    }
}