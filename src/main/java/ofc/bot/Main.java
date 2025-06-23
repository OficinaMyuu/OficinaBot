package ofc.bot;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import ofc.bot.domain.sqlite.DB;
import ofc.bot.handlers.ConsoleQueryHandler;
import ofc.bot.handlers.EntityInitializerManager;
import ofc.bot.internal.data.BotFiles;
import ofc.bot.twitch.TwitchService;
import ofc.bot.util.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static long initTime;
    private static JDA api;
    private static TwitchService twitchService;
    private static OpenAIClient openAI;

    public static void main(String[] args) {
        try {
            BotFiles.loadFiles();
            DB.init();

            api = JDABuilder.createDefault(Bot.getSafe("app.token"), Bot.getIntents())
                    .setEventPassthrough(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableCache(CacheFlag.VOICE_STATE, CacheFlag.ACTIVITY)
                    .setActivity(Activity.playing("Oficina"))
                    .build()
                    .awaitReady();

            initTime = Bot.unixNow();

            ConsoleQueryHandler.init();
            EntityInitializerManager.initializeCronJobs();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize some essential bot services", e);
            return;
        }

        // Registers
        EntityInitializerManager.initServices();
        EntityInitializerManager.registerListeners();
        EntityInitializerManager.registerSlashCommands();
        EntityInitializerManager.registerComposedInteractions();

        // Twitch
        twitchService = TwitchService.init();

        // OpenAI
        openAI = OpenAIOkHttpClient.builder()
                .apiKey(Bot.getSafe("openai.key"))
                .build();
    }

    public static JDA getApi() {
        return api;
    }

    public static TwitchService getTwitch() {
        return twitchService;
    }

    public static OpenAIClient getOpenAI() {
        return openAI;
    }

    public static long getInitTime() {
        return initTime;
    }
}