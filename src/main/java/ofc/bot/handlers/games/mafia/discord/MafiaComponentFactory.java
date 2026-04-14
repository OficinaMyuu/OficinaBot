package ofc.bot.handlers.games.mafia.discord;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import ofc.bot.handlers.games.mafia.domain.MafiaMatch;
import ofc.bot.handlers.games.mafia.domain.MafiaPlayer;
import ofc.bot.handlers.games.mafia.enums.MafiaRole;

import java.util.List;
import java.util.function.Predicate;

/**
 * Factory for Discord buttons and select menus used by Oficina Dorme.
 * <p>
 * The current UX relies on string select menus, which is why the feature enforces a hard 25-player cap.
 */
public final class MafiaComponentFactory {
    public static final String LOBBY_JOIN_BUTTON_ID = "mafia_lobby_join";
    public static final String LOBBY_LEAVE_BUTTON_ID = "mafia_lobby_leave";
    public static final String LOBBY_START_BUTTON_ID = "mafia_lobby_start";
    public static final String VIEW_ROLE_BUTTON_ID = "mafia_view_role";
    public static final String OPEN_DAY_VOTE_BUTTON_ID = "mafia_open_day_vote";
    public static final String RESOLVE_DAY_VOTE_BUTTON_ID = "mafia_resolve_day_vote";
    public static final String ASSASSIN_MENU_ID = "mafia_night_assassin_vote";
    public static final String DOCTOR_MENU_ID = "mafia_night_doctor_vote";
    public static final String DETECTIVE_MENU_ID = "mafia_night_detective_vote";
    public static final String DAY_VOTE_MENU_ID = "mafia_day_vote";

    /**
     * Utility class.
     */
    private MafiaComponentFactory() {}

    /**
     * Builds the lobby action buttons.
     *
     * @return join/leave/start buttons
     */
    public static List<Button> createLobbyButtons() {
        return List.of(
                Button.of(ButtonStyle.PRIMARY, LOBBY_JOIN_BUTTON_ID, "Participar"),
                Button.of(ButtonStyle.SECONDARY, LOBBY_LEAVE_BUTTON_ID, "Sair"),
                Button.of(ButtonStyle.SUCCESS, LOBBY_START_BUTTON_ID, "Começar")
        );
    }

    /**
     * Builds the private role-reveal button.
     *
     * @return role-reveal button
     */
    public static Button createViewRoleButton() {
        return Button.of(ButtonStyle.PRIMARY, VIEW_ROLE_BUTTON_ID, "Ver minha função");
    }

    /**
     * Builds the button that opens day voting.
     *
     * @return open-day-vote button
     */
    public static Button createOpenDayVoteButton() {
        return Button.of(ButtonStyle.SUCCESS, OPEN_DAY_VOTE_BUTTON_ID, "Abrir votação do dia");
    }

    /**
     * Builds the button that resolves day voting.
     *
     * @return resolve-day-vote button
     */
    public static Button createResolveDayVoteButton() {
        return Button.of(ButtonStyle.DANGER, RESOLVE_DAY_VOTE_BUTTON_ID, "Encerrar votação do dia");
    }

    /**
     * Builds the role-specific night menu for a private thread.
     *
     * @param guild guild that owns the match
     * @param match active match
     * @param role role acting in the thread
     * @return configured night vote menu
     */
    public static StringSelectMenu createNightVoteMenu(Guild guild, MafiaMatch match, MafiaRole role) {
        return switch (role) {
            case ASSASSIN -> createVoteMenu(
                    ASSASSIN_MENU_ID,
                    "Escolha quem será eliminado nesta noite",
                    guild,
                    match,
                    player -> player.isAlive() && player.getRole() != MafiaRole.ASSASSIN
            );
            case DOCTOR -> createVoteMenu(
                    DOCTOR_MENU_ID,
                    "Escolha quem você quer proteger",
                    guild,
                    match,
                    MafiaPlayer::isAlive
            );
            case DETECTIVE -> createVoteMenu(
                    DETECTIVE_MENU_ID,
                    "Escolha quem você quer investigar",
                    guild,
                    match,
                    MafiaPlayer::isAlive
            );
            case VILLAGER -> throw new IllegalArgumentException("Villagers do not have a night action menu.");
        };
    }

    /**
     * Builds the village day vote menu shown in the main channel.
     *
     * @param guild guild that owns the match
     * @param match active match
     * @return configured day vote menu
     */
    public static StringSelectMenu createDayVoteMenu(Guild guild, MafiaMatch match) {
        return createVoteMenu(
                DAY_VOTE_MENU_ID,
                "Escolha quem deve ser eliminado hoje",
                guild,
                match,
                MafiaPlayer::isAlive
        );
    }

    /**
     * Builds a generic one-target select menu from the current roster.
     *
     * @param id component id used by the listener
     * @param placeholder localized placeholder text
     * @param guild guild that owns the match
     * @param match active match
     * @param filter predicate used to constrain selectable targets
     * @return configured select menu
     */
    private static StringSelectMenu createVoteMenu(String id, String placeholder, Guild guild, MafiaMatch match,
                                                   Predicate<MafiaPlayer> filter) {
        List<SelectOption> options = match.getPlayers().stream()
                .filter(filter)
                .map(player -> SelectOption.of(resolveDisplayName(guild, player), String.valueOf(player.getUserId())))
                .limit(MafiaMatch.MAX_PLAYERS)
                .toList();

        return StringSelectMenu.create(id)
                .setPlaceholder(placeholder)
                .addOptions(options)
                .setMinValues(1)
                .setMaxValues(1)
                .build();
    }

    /**
     * Resolves a safe display label for one player option.
     *
     * @param guild guild that owns the match
     * @param player player being rendered
     * @return display label capped to Discord's select-menu limit
     */
    private static String resolveDisplayName(Guild guild, MafiaPlayer player) {
        Member member = guild.getMemberById(player.getUserId());
        String base = member == null ? player.getMention() : member.getEffectiveName();
        return base.length() > 100 ? base.substring(0, 100) : base;
    }
}
