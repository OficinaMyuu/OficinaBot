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
import ofc.bot.util.Bot;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DiscordCommand(name = "emojis authorize")
public class AuthorizeEmojiCommand extends SlashSubcommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizeEmojiCommand.class);
    private final MemberEmojiRepository emojiRepo;
    private final UserEmojiPermissionRepository emojiPermRepo;

    public AuthorizeEmojiCommand(MemberEmojiRepository emojiRepo, UserEmojiPermissionRepository emojiPermRepo) {
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

        if (actor == target)
            return Status.CANNOT_AUTHORIZE_EMOJIS_FOR_YOURSELF;

        boolean isAuthorized = emojiPermRepo.existsByUserAndEmoji(targetId, emoji.getEmoji());
        if (isAuthorized)
            return Status.EMOJI_ALREADY_AUTHORIZED_TO_USER.args(target.getAsMention());

        try {
            long now = Bot.nowMillis();
            UserEmojiPermission perm = new UserEmojiPermission(actorId, targetId, emoji.getEmoji(), now);

            emojiPermRepo.save(perm);
            return Status.EMOJI_PERMISSION_SUCCESSFULLY_GRANTED.args(emoji.getEmoji(), target.getAsMention());
        } catch (DataAccessException e) {
            LOGGER.error("Failed to save emoji authorization to {}", targetId, e);
            return Status.COULD_NOT_EXECUTE_SUCH_OPERATION;
        }
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Autoriza um membro a usar um emoji de sua titularidade.";
    }

    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "O usuário a ser autorizado.", true)
        );
    }
}