package ofc.bot.listeners.discord.guilds;

import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ofc.bot.domain.sqlite.repository.TempBanRepository;
import ofc.bot.util.content.annotations.listeners.DiscordEventHandler;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DiscordEventHandler
public class UnbanTempBanCleaner extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnbanTempBanCleaner.class);
    private final TempBanRepository tmpBanRepo;

    public UnbanTempBanCleaner(TempBanRepository tmpBanRepo) {
        this.tmpBanRepo = tmpBanRepo;
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent e) {
        long userId = e.getUser().getIdLong();
        long guildId = e.getGuild().getIdLong();

        try {
            tmpBanRepo.deleteByUserAndGuildId(userId, guildId);
        } catch (DataAccessException err) {
            LOGGER.error("Could not delete temporary ban for user {} and guild {}", userId, guildId, err);
        }
    }
}