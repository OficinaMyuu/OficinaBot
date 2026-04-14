package ofc.bot.commands.impl.slash.mafia;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ofc.bot.handlers.games.mafia.discord.MafiaComponentFactory;
import ofc.bot.handlers.games.mafia.discord.MafiaMessageFactory;
import ofc.bot.handlers.games.mafia.domain.MafiaMatch;
import ofc.bot.handlers.games.mafia.domain.MafiaRoleConfiguration;
import ofc.bot.handlers.games.mafia.enums.MafiaEventType;
import ofc.bot.handlers.games.mafia.service.MafiaGameManager;
import ofc.bot.handlers.games.mafia.service.MafiaGameLogger;
import ofc.bot.handlers.interactions.commands.contexts.impl.SlashCommandContext;
import ofc.bot.handlers.interactions.commands.responses.states.InteractionResult;
import ofc.bot.handlers.interactions.commands.responses.states.Status;
import ofc.bot.handlers.interactions.commands.slash.abstractions.SlashSubcommand;
import ofc.bot.util.content.annotations.commands.DiscordCommand;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implements the `/oficinadorme create` subcommand.
 * <p>
 * The command creates the lobby message in the current text channel and optionally accepts a manual special-role
 * configuration. The player cap is intentionally limited to 25 because the match uses Discord select menus for
 * every vote.
 */
@DiscordCommand(name = "oficinadorme create")
public class CreateMafiaGameCommand extends SlashSubcommand {
    private final MafiaGameManager gameManager = MafiaGameManager.getInstance();
    private final MafiaGameLogger gameLogger = MafiaGameLogger.getInstance();

    /**
     * Creates the lobby in the current guild text channel.
     *
     * @param ctx slash-command execution context
     * @return interaction response for the command
     */
    @Override
    public InteractionResult onCommand(@NotNull SlashCommandContext ctx) {
        if (!(ctx.getChannel() instanceof TextChannel textChannel)) {
            return ctx.reply("Este comando só pode ser usado em canais de texto do servidor.", true);
        }

        long channelId = textChannel.getIdLong();
        if (gameManager.hasActiveMatch(channelId)) {
            return Status.GAME_ALREADY_RUNNING_IN_CHANNEL;
        }

        int maxPlayers = ctx.getOption("max-players", MafiaMatch.MAX_PLAYERS, OptionMapping::getAsInt);
        Integer assassins = ctx.getOption("assassins", OptionMapping::getAsInt);
        Integer doctors = ctx.getOption("doctors", OptionMapping::getAsInt);
        Integer detectives = ctx.getOption("detectives", OptionMapping::getAsInt);

        MafiaRoleConfiguration requestedConfiguration = buildRequestedConfiguration(assassins, doctors, detectives);
        if ((assassins != null || doctors != null || detectives != null) && requestedConfiguration == null) {
            return ctx.reply("Se você quiser definir os papéis manualmente, informe **assassins**, **doctors** e **detectives** juntos.", true);
        }

        var requestedConfigurationValidation = gameManager.getMatchEngine().validateRequestedConfiguration(requestedConfiguration);
        if (requestedConfigurationValidation.isPresent()) {
            return ctx.reply(requestedConfigurationValidation.get(), true);
        }

        if (requestedConfiguration != null) {
            var maxPlayersValidation = gameManager.getMatchEngine().validateConfiguration(requestedConfiguration, maxPlayers);
            if (maxPlayersValidation.isPresent()) {
                return ctx.reply(maxPlayersValidation.get(), true);
            }
        }

        MafiaMatch match = gameManager.createMatch(
                ctx.getGuildId(),
                channelId,
                ctx.getIssuer().getIdLong(),
                maxPlayers,
                requestedConfiguration
        );
        gameLogger.log(
                match,
                MafiaEventType.LOBBY_CREATED,
                "Lobby created.",
                ctx.getIssuer().getIdLong(),
                null,
                channelId
        );

        return ctx.create()
                .setEmbeds(MafiaMessageFactory.createLobbyEmbed(match))
                .setActionRows(MafiaComponentFactory.createLobbyButtons())
                .onSend(hook -> hook.retrieveOriginal().queue(message -> match.setLobbyMessageId(message.getIdLong())))
                .send();
    }

    /**
     * Returns the localized description used during slash-command registration.
     *
     * @return pt-BR description
     */
    @NotNull
    @Override
    public String getDescription() {
        return "Cria um lobby de Oficina Dorme no canal atual.";
    }

    /**
     * Returns the supported slash-command options.
     *
     * @return command options
     */
    @NotNull
    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.INTEGER, "max-players", "Quantidade máxima de jogadores da partida (até 25).")
                        .setMinValue(MafiaMatch.MIN_PLAYERS)
                        .setMaxValue(MafiaMatch.MAX_PLAYERS),
                new OptionData(OptionType.INTEGER, "assassins", "Quantidade manual de assassinos.")
                        .setMinValue(2)
                        .setMaxValue(3),
                new OptionData(OptionType.INTEGER, "doctors", "Quantidade manual de médicos.")
                        .setMinValue(1)
                        .setMaxValue(2),
                new OptionData(OptionType.INTEGER, "detectives", "Quantidade manual de detetives.")
                        .setMinValue(1)
                        .setMaxValue(2)
        );
    }

    /**
     * Creates a manual role configuration only when all manual values are supplied together.
     *
     * @param assassins requested assassin count
     * @param doctors requested doctor count
     * @param detectives requested detective count
     * @return manual configuration, or {@code null} when auto-balance should be used
     */
    private MafiaRoleConfiguration buildRequestedConfiguration(Integer assassins, Integer doctors, Integer detectives) {
        if (assassins == null && doctors == null && detectives == null) {
            return null;
        }

        if (assassins == null || doctors == null || detectives == null) {
            return null;
        }

        return new MafiaRoleConfiguration(assassins, doctors, detectives);
    }
}
