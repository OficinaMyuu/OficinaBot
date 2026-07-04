package ofc.bot.handlers.moderation;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.internal.utils.Checks;
import ofc.bot.domain.entity.AutomodAction;
import ofc.bot.domain.entity.MemberPunishment;
import ofc.bot.domain.entity.enums.PunishmentType;
import ofc.bot.domain.database.repository.AutomodActionRepository;
import ofc.bot.domain.database.repository.MemberPunishmentRepository;
import ofc.bot.handlers.exceptions.PunishmentCreationException;
import ofc.bot.util.embeds.EmbedFactory;
import org.jetbrains.annotations.NotNull;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public final class PunishmentManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PunishmentManager.class);
    private static final long PUNISHMENT_EXPIRY_SECONDS = TimeUnit.DAYS.toSeconds(180); // 6 months
    private final MemberPunishmentRepository pnshRepo;
    private final AutomodActionRepository modActRepo;
    private final AutoKickCleanup autoKickCleanup;

    public PunishmentManager(MemberPunishmentRepository pnshRepo, AutomodActionRepository modActRepo) {
        this(pnshRepo, modActRepo, AutoKickCleanup.createDefault());
    }

    PunishmentManager(
            MemberPunishmentRepository pnshRepo,
            AutomodActionRepository modActRepo,
            AutoKickCleanup autoKickCleanup
    ) {
        this.pnshRepo = pnshRepo;
        this.modActRepo = modActRepo;
        this.autoKickCleanup = autoKickCleanup;
    }

    public MessageEmbed createPunishment(@NotNull PunishmentData data) {
        Checks.notNull(data, "WarnData");
        Reason reason = data.reason();
        Member target = data.target();
        Member author = data.author();
        String fmtReason = reason.toString();
        MemberPunishment punishment = MemberPunishment.fromMember(target, author.getIdLong(), fmtReason);

        try {
            pnshRepo.upsert(punishment);
            int warnCount = pnshRepo.countByUserIdAfter(target.getIdLong(), PUNISHMENT_EXPIRY_SECONDS);
            AutomodAction automodAction = modActRepo.findLastByThreshold(warnCount);

            if (automodAction == null)
                throw new IllegalStateException("No Automod actions defined");

            PunishmentType action = automodAction.getAction();
            int duration = automodAction.getDurationRaw();
            resolveAction(target, duration, action)
                    .reason(fmtReason)
                    .queue(ignored -> resetOnKick(target, action, fmtReason));

            PunishmentType embedAction = action == PunishmentType.MUTE ? PunishmentType.WARN : action;
            return EmbedFactory.embedPunishment(target.getUser(), embedAction, fmtReason);
        } catch (DataAccessException e) {
            throw new PunishmentCreationException(
                    "Could not create punisment for member " + punishment.getUserId(), e);
        }
    }

    private AuditableRestAction<?> resolveAction(Member target, int duration, PunishmentType type) {
        return switch (type) {
            case WARN, MUTE -> target.timeoutFor(duration, TimeUnit.SECONDS);
            case KICK -> target.kick();
            case BAN -> target.ban(0, TimeUnit.SECONDS);

            // This will never happen
            case UNMUTE, UNBAN -> throw new UnsupportedOperationException();
        };
    }

    private void resetOnKick(Member target, PunishmentType action, String reason) {
        if (action != PunishmentType.KICK) return;

        long userId = target.getIdLong();
        long guildId = target.getGuild().getIdLong();
        try {
            autoKickCleanup.reset(userId, guildId, reason);
        } catch (RuntimeException e) {
            LOGGER.error("Could not reset XP and economy after auto-kicking member {}", userId, e);
        }
    }
}
