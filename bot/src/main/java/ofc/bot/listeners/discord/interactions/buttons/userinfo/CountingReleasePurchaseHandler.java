package ofc.bot.listeners.discord.interactions.buttons.userinfo;

import net.dv8tion.jda.api.entities.*;
import ofc.bot.handlers.economy.BankAction;
import ofc.bot.handlers.economy.CurrencyType;
import ofc.bot.handlers.interactions.AutoResponseType;
import ofc.bot.handlers.interactions.InteractionListener;
import ofc.bot.handlers.interactions.buttons.contexts.ButtonClickContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.userinfo.CountingReleaseService;
import ofc.bot.handlers.userinfo.CountingReleaseService.ReleaseAttempt;
import ofc.bot.util.Scopes;
import ofc.bot.util.content.annotations.listeners.InteractionHandler;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.OptionalLong;

@InteractionHandler(scope = Scopes.Userinfo.RELEASE_COUNTING_PUNISHMENT, autoResponseType = AutoResponseType.DEFER_EDIT)
public class CountingReleasePurchaseHandler implements InteractionListener<ButtonClickContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountingReleasePurchaseHandler.class);
    private final CountingReleaseService service;

    public CountingReleasePurchaseHandler(CountingReleaseService service) {
        this.service = service;
    }

    @Override
    public InteractionResult onExecute(ButtonClickContext ctx) {
        Guild guild = ctx.getGuild();
        User user = ctx.get("user");
        Member member = ctx.getIssuer();
        CurrencyType currency = ctx.get("currency");
        OptionalLong roleId = service.findPunishmentRoleId();

        if (roleId.isEmpty()) {
            return Status.ROLE_NOT_FOUND;
        }

        Role role = guild.getRoleById(roleId.getAsLong());
        if (role == null) {
            return Status.ROLE_NOT_FOUND;
        }

        ReleaseAttempt attempt = service.charge(
                currency,
                user.getIdLong(),
                guild.getIdLong(),
                member.getRoles().contains(role)
        );

        return switch (attempt.result()) {
            case ALREADY_RELEASED -> ctx.reply("Voce ja esta liberado da contagem.", true);
            case INSUFFICIENT_BALANCE -> Status.INSUFFICIENT_BALANCE;
            case CONFIGURATION_UNAVAILABLE -> Status.STORE_ITEM_CONFIGURATION_UNAVAILABLE;
            case CHARGED -> releaseRole(ctx, guild, user, role, currency, attempt.action());
        };
    }

    private InteractionResult releaseRole(
            ButtonClickContext ctx,
            Guild guild,
            User user,
            Role role,
            CurrencyType currency,
            BankAction charge
    ) {
        guild.removeRoleFromMember(user, role).queue(success -> {
            MessageEmbed embed = EmbedFactory.embedCountingReleaseSuccess(user, role, currency);
            ctx.replyEmbeds(embed);
            ctx.disableAll();
        }, error -> {
            LOGGER.error("Failed to release counting punishment role {} from user {}", role.getId(), user.getId(), error);
            charge.rollback();
            ctx.reply(Status.COULD_NOT_EXECUTE_SUCH_OPERATION);
        });

        return Status.OK;
    }
}
