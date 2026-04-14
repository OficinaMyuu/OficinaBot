package ofc.bot.handlers.games.mafia.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ofc.bot.handlers.games.mafia.domain.DayResolution;
import ofc.bot.handlers.games.mafia.domain.MafiaMatch;
import ofc.bot.handlers.games.mafia.domain.MafiaPlayer;
import ofc.bot.handlers.games.mafia.domain.MafiaRoleConfiguration;
import ofc.bot.handlers.games.mafia.domain.NightInvestigationResult;
import ofc.bot.handlers.games.mafia.domain.NightResolution;
import ofc.bot.handlers.games.mafia.enums.MafiaRole;
import ofc.bot.handlers.games.mafia.enums.MafiaTeam;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for Discord embeds used by Oficina Dorme.
 * <p>
 * All user-facing strings emitted by the mafia feature should be centralized here so the Discord copy stays in
 * pt-BR even though the codebase itself is written in English.
 */
public final class MafiaMessageFactory {
    /**
     * Utility class.
     */
    private MafiaMessageFactory() {}

    /**
     * Builds the lobby embed shown in the main event channel.
     *
     * @param match lobby match
     * @return ready-to-send embed
     */
    public static MessageEmbed createLobbyEmbed(MafiaMatch match) {
        String configuration = formatRequestedConfiguration(match);
        String participants = match.getPlayers().isEmpty()
                ? "Ninguém entrou na partida ainda."
                : match.getPlayers().stream()
                        .map(MafiaPlayer::getMention)
                        .collect(Collectors.joining("\n"));

        int missingPlayers = Math.max(0, MafiaMatch.MIN_PLAYERS - match.getPlayerCount());
        String status = missingPlayers == 0
                ? "Jogadores suficientes para começar. A staff ou o host já pode iniciar."
                : "Faltam **" + missingPlayers + "** jogadores para atingir o mínimo.";

        return new EmbedBuilder()
                .setTitle("Oficina Dorme")
                .setColor(0x2b2d31)
                .setDescription("""
                        As inscrições da partida estão abertas.

                        Clique em **Participar** para entrar no jogo ou em **Sair** para deixar a lista.
                        Quando estiver tudo certo, a staff ou o host pode clicar em **Começar**.
                        """ + "\n" + status)
                .addField("Host", "<@" + match.getHostId() + ">", true)
                .addField("Jogadores", match.getPlayerCount() + "/" + match.getMaxPlayers(), true)
                .addField("Configuração", configuration, false)
                .addField("Participantes", participants, false)
                .setFooter("Limite técnico atual: até 25 jogadores por partida, por causa do limite de opções do menu.")
                .build();
    }

    /**
     * Builds the private role-reveal embed sent to one participant.
     *
     * @param role role to describe
     * @return ready-to-send embed
     */
    public static MessageEmbed createRoleRevealEmbed(MafiaRole role) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Sua função: " + role.getDisplayName());

        switch (role) {
            case ASSASSIN -> builder
                    .setColor(0xed4245)
                    .setDescription("""
                            Você é um **Assassino**.

                            **Objetivo:** eliminar a aldeia.
                            **Durante a noite:** cada assassino escolhe 1 jogador para matar.
                            **Durante o dia:** vote normalmente com o restante da vila.
                            **Vitória:** os assassinos vencem quando a quantidade de membros da aldeia vivos se iguala à quantidade de assassinos vivos.
                            """);
            case DOCTOR -> builder
                    .setColor(0x57f287)
                    .setDescription("""
                            Você é um **Médico**.

                            **Objetivo:** manter a aldeia viva.
                            **Durante a noite:** escolha 1 jogador para proteger.
                            - Seu alvo fica protegido de investigações nesta noite.
                            - Seu alvo também fica protegido de assassinato, exceto se o alvo for você mesmo.
                            - Você não pode escolher a mesma pessoa em duas noites seguidas.
                            **Durante o dia:** vote com a vila.
                            """);
            case DETECTIVE -> builder
                    .setColor(0x5865f2)
                    .setDescription("""
                            Você é um **Detetive**.

                            **Objetivo:** descobrir quem faz parte dos assassinos.
                            **Durante a noite:** escolha 1 jogador para investigar.
                            - Se o alvo não for protegido nem morto nesta noite, o bot revela apenas o **time** dele para os detetives.
                            - Se o alvo for protegido ou morrer, a investigação falha.
                            - Você não pode investigar a mesma pessoa em duas noites seguidas.
                            **Durante o dia:** vote com a vila.
                            """);
            case VILLAGER -> builder
                    .setColor(0x95a5a6)
                    .setDescription("""
                            Você é um **Aldeão**.

                            **Objetivo:** eliminar todos os assassinos.
                            **Durante a noite:** você não faz nada.
                            **Durante o dia:** converse, suspeite e vote em quem você acredita que seja assassino.
                            """);
        }

        return builder.build();
    }

    /**
     * Builds the public embed that announces the start of a night round.
     *
     * @param match active match
     * @return ready-to-send embed
     */
    public static MessageEmbed createNightAnnouncement(MafiaMatch match) {
        return new EmbedBuilder()
                .setTitle("Noite " + match.getNightNumber())
                .setColor(0x111827)
                .setDescription("""
                        A cidade foi dormir.

                        As funções especiais já podem agir nas threads privadas.
                        Os apresentadores podem narrar a rodada enquanto o bot aguarda as decisões da noite.
                        """)
                .addField("Jogadores vivos", String.valueOf(match.getAlivePlayerCount()), true)
                .addField("Participantes", formatRoster(match), false)
                .build();
    }

    /**
     * Builds the private action prompt for one role thread during the night.
     *
     * @param match active match
     * @param role role that owns the thread
     * @return ready-to-send embed
     */
    public static MessageEmbed createNightPrompt(MafiaMatch match, MafiaRole role) {
        return createNightPrompt(match, role, false);
    }

    /**
     * Builds the refreshed private action prompt posted after somebody leaves during the night.
     *
     * @param match active match
     * @param role role that owns the thread
     * @return ready-to-send embed
     */
    public static MessageEmbed createNightPromptRefresh(MafiaMatch match, MafiaRole role) {
        return createNightPrompt(match, role, true);
    }

    /**
     * Builds the detective-only night result embed.
     *
     * @param match active match
     * @param resolution resolved night result
     * @return ready-to-send embed
     */
    public static MessageEmbed createDetectiveResults(MafiaMatch match, NightResolution resolution) {
        String results = resolution.investigationsByDetective().values().stream()
                .map(MafiaMessageFactory::formatInvestigationResult)
                .collect(Collectors.joining("\n"));

        return new EmbedBuilder()
                .setTitle("Resultados da investigação - noite " + match.getNightNumber())
                .setColor(0x5865f2)
                .setDescription(results.isBlank() ? "Nenhuma investigação foi concluída nesta noite." : results)
                .build();
    }

    /**
     * Builds the daytime discussion embed shown after the night resolves.
     *
     * @param match active match
     * @param killedPlayers players killed during the resolved night
     * @return ready-to-send embed
     */
    public static MessageEmbed createDayDiscussion(MafiaMatch match, Set<Long> killedPlayers) {
        String nightSummary = killedPlayers.isEmpty()
                ? "Ninguém morreu durante a noite."
                : "Mortes da noite: " + mentionPlayers(killedPlayers) + ".";

        return new EmbedBuilder()
                .setTitle("Dia " + match.getDayNumber())
                .setColor(0xf59e0b)
                .setDescription("""
                        A cidade acordou.

                        %s

                        Quando os apresentadores terminarem de conduzir a discussão, a staff ou o host pode abrir a votação do dia.
                        """.formatted(nightSummary))
                .addField("Participantes", formatRoster(match), false)
                .build();
    }

    /**
     * Builds the embed used when day voting opens.
     *
     * @param match active match
     * @return ready-to-send embed
     */
    public static MessageEmbed createDayVote(MafiaMatch match) {
        return new EmbedBuilder()
                .setTitle("Votação do dia " + match.getDayNumber())
                .setColor(0xfee75c)
                .setDescription("""
                        A votação está aberta.

                        Cada jogador vivo deve escolher 1 pessoa para ser eliminada.
                        Em caso de empate, ninguém morre.
                        """)
                .addField("Participantes", formatRoster(match), false)
                .build();
    }

    /**
     * Builds the embed used when day voting is refreshed after the alive roster changes.
     *
     * @param match active match
     * @return ready-to-send embed
     */
    public static MessageEmbed createDayVoteRefresh(MafiaMatch match) {
        return new EmbedBuilder()
                .setTitle("Votação do dia " + match.getDayNumber() + " - lista atualizada")
                .setColor(0xfee75c)
                .setDescription("""
                        Um participante deixou a partida e a lista de votação foi atualizada.

                        Se o seu voto anterior ficou inválido, selecione novamente usando o menu mais recente.
                        Em caso de empate, ninguém morre.
                        """)
                .addField("Participantes", formatRoster(match), false)
                .build();
    }

    /**
     * Builds the embed used when day voting resolves.
     *
     * @param match active match
     * @param resolution resolved day result
     * @return ready-to-send embed
     */
    public static MessageEmbed createDayResolution(MafiaMatch match, DayResolution resolution) {
        String description = resolution.hasElimination()
                ? "O jogador mais votado foi " + mentionPlayer(resolution.eliminatedPlayerId()) + "."
                : "A votação terminou empatada ou sem votos válidos. Ninguém foi eliminado.";

        return new EmbedBuilder()
                .setTitle("Resultado do dia " + match.getDayNumber())
                .setColor(0xf97316)
                .setDescription(description)
                .build();
    }

    /**
     * Builds the final winner announcement embed.
     *
     * @param match finished match
     * @param winner winning team
     * @return ready-to-send embed
     */
    public static MessageEmbed createGameOver(MafiaMatch match, MafiaTeam winner) {
        return new EmbedBuilder()
                .setTitle("Fim de jogo")
                .setColor(winner == MafiaTeam.ASSASSINS ? 0xed4245 : 0x57f287)
                .setDescription("O time vencedor foi **" + winner.getDisplayName() + "**.")
                .addField("Participantes finais", formatRoster(match), false)
                .setFooter("As threads privadas permanecerão disponíveis até a staff decidir apagá-las.")
                .build();
    }

    /**
     * Builds a public notice for a player who left the guild or became unavailable.
     *
     * @param userId removed player id
     * @param reason localized removal reason
     * @return ready-to-send embed
     */
    public static MessageEmbed createPlayerUnavailableNotice(long userId, String reason) {
        return new EmbedBuilder()
                .setTitle("Participante removido")
                .setColor(0x5865f2)
                .setDescription("O jogador <@" + userId + "> " + reason + " e foi removido da partida.")
                .build();
    }

    /**
     * Builds the announcement used when a departure immediately ends the match.
     *
     * @param winner winning team after the departure
     * @param lastAssassinDeparted whether the departure specifically removed the final alive assassin
     * @return ready-to-send embed
     */
    public static MessageEmbed createDepartureVictoryNotice(MafiaTeam winner, boolean lastAssassinDeparted) {
        String description = lastAssassinDeparted
                ? "O último assassino deixou a partida. Os sobreviventes venceram e o jogo foi encerrado."
                : "A saída de participantes alterou as condições de vitória da partida. O jogo foi encerrado automaticamente.";

        return new EmbedBuilder()
                .setTitle("Partida encerrada")
                .setColor(winner == MafiaTeam.ASSASSINS ? 0xed4245 : 0x57f287)
                .setDescription(description)
                .build();
    }

    /**
     * Builds the notice used when a required channel or thread is deleted and the match can no longer continue.
     *
     * @param deletedChannelId deleted channel id
     * @param mainChannelDeleted whether the deleted channel was the main event channel
     * @return ready-to-send embed
     */
    public static MessageEmbed createChannelDeletedTermination(long deletedChannelId, boolean mainChannelDeleted) {
        String description = mainChannelDeleted
                ? "A partida foi encerrada porque o canal principal foi apagado. Sem ele, não dá para continuar o jogo com segurança."
                : "A partida foi encerrada porque um dos canais de ação foi apagado. Sem esse canal, não dá para continuar o jogo com segurança.";

        return new EmbedBuilder()
                .setTitle("Partida encerrada")
                .setColor(0xed4245)
                .setDescription(description + "\n\nCanal afetado: `" + deletedChannelId + "`.")
                .build();
    }

    /**
     * Formats the requested lobby configuration for display.
     *
     * @param match lobby match
     * @return formatted configuration text
     */
    private static String formatRequestedConfiguration(MafiaMatch match) {
        MafiaRoleConfiguration configuration = match.getRequestedRoleConfiguration();
        if (configuration == null) {
            return "Automática. O bot decide os papéis no momento do início.";
        }

        return """
                Assassinos: %d
                Médicos: %d
                Detetives: %d
                Aldeões: o restante
                """.formatted(configuration.assassins(), configuration.doctors(), configuration.detectives());
    }

    /**
     * Formats the full roster using colored alive/dead markers.
     *
     * @param match match whose roster should be rendered
     * @return formatted roster text
     */
    private static String formatRoster(MafiaMatch match) {
        return match.getPlayers().stream()
                .map(player -> "%s %s".formatted(player.isAlive() ? "🟢" : "🔴", player.getMention()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Formats one detective result line.
     *
     * @param result detective result to render
     * @return formatted result line
     */
    private static String formatInvestigationResult(NightInvestigationResult result) {
        if (result.blocked()) {
            return "%s investigou %s, mas não houve resultado.".formatted(
                    mentionPlayer(result.detectiveId()),
                    mentionPlayer(result.targetId())
            );
        }

        return "%s investigou %s e descobriu o time **%s**.".formatted(
                mentionPlayer(result.detectiveId()),
                mentionPlayer(result.targetId()),
                result.revealedTeam() == null ? "Desconhecido" : result.revealedTeam().getDisplayName()
        );
    }

    /**
     * Returns the embed color associated with one role.
     *
     * @param role role being rendered
     * @return embed color
     */
    private static int colorForRole(MafiaRole role) {
        return switch (role) {
            case ASSASSIN -> 0xed4245;
            case DOCTOR -> 0x57f287;
            case DETECTIVE -> 0x5865f2;
            case VILLAGER -> 0x95a5a6;
        };
    }

    /**
     * Formats a collection of player ids as Discord mentions.
     *
     * @param playerIds player ids to render
     * @return comma-separated mentions
     */
    private static String mentionPlayers(Collection<Long> playerIds) {
        return playerIds.stream()
                .map(MafiaMessageFactory::mentionPlayer)
                .collect(Collectors.joining(", "));
    }

    /**
     * Formats one player id as a Discord mention.
     *
     * @param playerId player id to render
     * @return mention string, or a localized fallback when the id is {@code null}
     */
    private static String mentionPlayer(@Nullable Long playerId) {
        return playerId == null ? "ninguém" : "<@" + playerId + ">";
    }

    /**
     * Builds the role-thread prompt used during the night and during roster refreshes.
     *
     * @param match active match
     * @param role role that owns the thread
     * @param refreshed whether the message is a refreshed version
     * @return ready-to-send embed
     */
    private static MessageEmbed createNightPrompt(MafiaMatch match, MafiaRole role, boolean refreshed) {
        String prefix = refreshed ? "Lista atualizada\n\n" : "";
        String description = switch (role) {
            case ASSASSIN -> prefix + """
                    Cada assassino deve escolher 1 alvo.
                    Não é permitido eliminar alguém do próprio time.
                    Se o seu alvo anterior deixou a partida, escolha novamente usando este menu.
                    """;
            case DOCTOR -> prefix + """
                    Cada médico deve escolher 1 alvo.
                    Você pode proteger qualquer jogador de investigação, mas não pode se proteger de assassinato.
                    Se o seu alvo anterior deixou a partida, escolha novamente usando este menu.
                    """;
            case DETECTIVE -> prefix + """
                    Cada detetive deve escolher 1 alvo.
                    O resultado será enviado apenas nesta thread.
                    Se o seu alvo anterior deixou a partida, escolha novamente usando este menu.
                    """;
            case VILLAGER -> "";
        };

        return new EmbedBuilder()
                .setTitle(role.getDisplayName() + " - noite " + match.getNightNumber())
                .setColor(colorForRole(role))
                .setDescription(description)
                .addField("Participantes da partida", formatRoster(match), false)
                .build();
    }
}
