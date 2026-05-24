package ofc.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import ofc.bot.commands.CreateRegister;
import ofc.bot.commands.RevokeRegister;
import ofc.bot.database.DB;
import ofc.bot.internal.BotData;
import ofc.bot.listeners.DeleteFormerMemberMessages;
import ofc.bot.listeners.NumberlessMessagesRemover;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegisterMaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterMaster.class);
    private static JDA api;

    public static void main(String[] args) {
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");

        try {
            DB.init();

            api = JDABuilder.createLight(BotData.get("app.token"))
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS
                    )
                    .setMemberCachePolicy(MemberCachePolicy.NONE)
                    .setActivity(Activity.playing("Registro"))
                    .build()
                    .awaitReady();

            api.addEventListener(
                    new CreateRegister(),
                    new RevokeRegister(),

                    new NumberlessMessagesRemover(),
                    new DeleteFormerMemberMessages()
            );

        } catch (InterruptedException e) {
            LOGGER.error("Could not login, exiting", e);
        } catch (DataAccessException e) {
            LOGGER.error("Could not initialize database", e);
        }
    }

    public static JDA getApi() {
        return api;
    }
}