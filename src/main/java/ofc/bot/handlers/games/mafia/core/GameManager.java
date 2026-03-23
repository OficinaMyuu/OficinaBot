package ofc.bot.handlers.games.mafia.core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import ofc.bot.handlers.games.mafia.enums.MatchPhase;
import ofc.bot.handlers.games.mafia.enums.Role;
import ofc.bot.handlers.games.mafia.models.MatchState;
import ofc.bot.handlers.games.mafia.models.Player;
import ofc.bot.handlers.games.mafia.utils.RoleCalculator;
import ofc.bot.util.Bot;
import ofc.bot.util.embeds.EmbedFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameManager.class);
    // Maps the main channel id to the active MatchState
    private final Map<Long, MatchState> activeMatches = new ConcurrentHashMap<>();

    public void createMatch(long channelId, MatchState match) {
        activeMatches.put(channelId, match);
    }

    public MatchState getMatch(long channelId) {
        return activeMatches.get(channelId);
    }

    public void endMatch(long channelId) {
        activeMatches.remove(channelId);
    }

    public boolean hasActiveMatch(long channelId) {
        return activeMatches.containsKey(channelId);
    }

    public void startMatch(MatchState match, Guild guild, InteractionHook hook) {
        match.setPhase(MatchPhase.NIGHT);

        List<Player> players = new ArrayList<>(match.getPlayers());
        Collections.shuffle(players);

        RoleCalculator.RoleDistribution distribution = RoleCalculator.calculate(players.size());
        assignRoles(players, distribution);

        TextChannel mainChannel = guild.getTextChannelById(match.getMainChannelId());
        if (mainChannel == null) {
            hook.editOriginal("Erro: O canal principal da partida não foi encontrado.").queue();
            return;
        }

        Category category = mainChannel.getParentCategory();
        long botId = guild.getJDA().getSelfUser().getIdLong();
        long denyAll = Permission.VIEW_CHANNEL.getRawValue();
        long allowBot = Permission.getRaw(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);

        ChannelAction<TextChannel> assassinsAction = buildRoleChannel(guild, category, "🔪-assassinos", Role.ASSASSIN, players, botId, denyAll, allowBot);
        ChannelAction<TextChannel> doctorsAction = buildRoleChannel(guild, category, "💊-medicos", Role.DOCTOR, players, botId, denyAll, allowBot);
        ChannelAction<TextChannel> detectivesAction = buildRoleChannel(guild, category, "🔍-detetives", Role.DETECTIVE, players, botId, denyAll, allowBot);

        RestAction.allOf(assassinsAction, doctorsAction, detectivesAction).queue(chans -> {
            TextChannel assassinsChan = chans.get(0);
            TextChannel doctorsChan = chans.get(1);
            TextChannel detectivesChan = chans.get(2);

            match.setAssassinsChannelId(assassinsChan.getIdLong());
            match.setDoctorsChannelId(doctorsChan.getIdLong());
            match.setDetectivesChannelId(detectivesChan.getIdLong());

            pingPlayersInRoleChannels(assassinsChan, getPlayersByRole(players, Role.ASSASSIN));
            pingPlayersInRoleChannels(doctorsChan, getPlayersByRole(players, Role.DOCTOR));
            pingPlayersInRoleChannels(detectivesChan, getPlayersByRole(players, Role.DETECTIVE));

            MessageEmbed embed = EmbedFactory.embedMafiaNightPhase(1, match.getPlayerCount());
            Button viewRoleBtn = Button.of(ButtonStyle.PRIMARY, "mafia_view_role", "Ver Função", Bot.Emojis.MAFIA_ROLE);

            mainChannel.sendMessageEmbeds(embed)
                    .addComponents(ActionRow.of(viewRoleBtn))
                    .queue();

            hook.editOriginal("Partida iniciada com sucesso! As funções foram enviadas no privado.").queue();
        }, (err) -> {
            LOGGER.error("Failed to create private channels for Mafia game", err);
            hook.editOriginal("Ocorreu um erro ao criar os canais secretos.").queue();
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ChannelAction<TextChannel> buildRoleChannel(Guild guild, Category category, String name,
                                                        Role role, List<Player> players, long botId,
                                                        long denyAll, long allowBot) {
        ChannelAction<TextChannel> action = guild.createTextChannel(name, category)
                .addPermissionOverride(guild.getPublicRole(), 0, denyAll)
                .addMemberPermissionOverride(botId, allowBot, 0);

        for (Player p : getPlayersByRole(players, role)) {
            action.addMemberPermissionOverride(p.getUserId(), allowBot, 0);
        }

        return action;
    }

    private void assignRoles(List<Player> shuffledPlayers, RoleCalculator.RoleDistribution dist) {
        int index = 0;

        for (int i = 0; i < dist.assassins(); i++) {
            shuffledPlayers.get(index++).setRole(Role.ASSASSIN);
        }

        for (int i = 0; i < dist.doctors(); i++) {
            shuffledPlayers.get(index++).setRole(Role.DOCTOR);
        }

        for (int i = 0; i < dist.detectives(); i++) {
            shuffledPlayers.get(index++).setRole(Role.DETECTIVE);
        }

        while (index < shuffledPlayers.size()) {
            shuffledPlayers.get(index++).setRole(Role.VILLAGER);
        }
    }

    private List<Player> getPlayersByRole(List<Player> players, Role role) {
        return players.stream()
                .filter(p -> p.getRole() == role)
                .toList();
    }

    private void pingPlayersInRoleChannels(TextChannel channel, List<Player> players) {
        if (players.isEmpty()) return;

        String mentions = players.stream()
                .map(Player::getMention)
                .collect(Collectors.joining(" "));

        channel.sendMessageFormat("%s\n> Bem-vindos! Vocês são os membros desta função.", mentions).queue();
    }
}